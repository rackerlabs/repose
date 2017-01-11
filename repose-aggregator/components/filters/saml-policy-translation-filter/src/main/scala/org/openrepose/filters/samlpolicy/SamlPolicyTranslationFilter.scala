/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */

package org.openrepose.filters.samlpolicy

import java.io.InputStream
import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletInputStream, ServletRequest, ServletResponse}
import javax.ws.rs.core.MediaType

import com.google.common.cache.{Cache, LoadingCache}
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.sf.saxon.s9api.XsltExecutable
import org.openrepose.commons.utils.http.CommonHttpHeader.{CONTENT_LENGTH, CONTENT_TYPE}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientFactory
import org.openrepose.filters.samlpolicy.config.SamlPolicyConfig
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, LifecycleEvents}
import org.w3c.dom.Document

/**
  * Created by adrian on 12/12/16.
  */
@Named
class SamlPolicyTranslationFilter @Inject()(configurationService: ConfigurationService,
                                            atomFeedService: AtomFeedService,
                                            akkaServiceClientFactory: AkkaServiceClientFactory)
  extends AbstractConfiguredFilter[SamlPolicyConfig](configurationService)
  with LazyLogging
  with AtomFeedListener {

  override val DEFAULT_CONFIG: String = "saml-policy.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/config/schema/saml-policy.xsd"

  private var cache: LoadingCache[String, XsltExecutable] = _
  private var feedId: Option[String] = None

  override def doWork(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    try {
      val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      var response = servletResponse.asInstanceOf[HttpServletResponse]
      val samlResponse = decodeSamlResponse(request)
      val rawDocument = readToDom(samlResponse)

      val version = determineVersion(rawDocument)
      val finalDocument = version match {
        case 1 =>
          request.addHeader("Identity-API-Version", "1.0")
          rawDocument
        case 2 =>
          response = new HttpServletResponseWrapper(response, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)
          request.addHeader("Identity-API-Version", "2.0")
          val issuer = validateResponseAndGetIssuer(rawDocument)
          val translatedDocument = translateResponse(rawDocument, cache.get(issuer))
          signResponse(translatedDocument)
      }
      val inputStream = convertDocumentToStream(finalDocument)

      request.replaceHeader(CONTENT_TYPE.toString, MediaType.APPLICATION_XML)
      request.removeHeader(CONTENT_LENGTH.toString)
      request.replaceHeader("Transfer-Encoding", "chunked")

      chain.doFilter(new HttpServletRequestWrapper(request, inputStream), response)

      if (version == 2) {
        doResponseStuff()
        response.asInstanceOf[HttpServletResponseWrapper].commitToResponse()
      }
    } catch {
      case ex: SamlPolicyException =>
        servletResponse.asInstanceOf[HttpServletResponse].sendError(ex.statusCode, ex.message)
        logger.debug("SAML policy translation failed", ex)
      case ex: Exception =>
        servletResponse.asInstanceOf[HttpServletResponse].sendError(SC_INTERNAL_SERVER_ERROR, "Unknown error in SAML Policy Filter")
        logger.warn("Unexpected problem in SAML Policy Filter", ex)
    }
  }

  /**
    * Gets the SAMl response from the post encoded body, and decodes it into a plain String.
    *
    * @param request the servlet request that has an encoded body
    * @return the decoded saml response
    * @throws SamlPolicyException if decoding fails
    */
  def decodeSamlResponse(request: HttpServletRequest): InputStream = ???

  /**
    * Parses a saml response into a dom document.
    *
    * @param samlResponse the decoded saml response
    * @return the corresponding dom object
    * @throws SamlPolicyException if parsing fails
    */
  def readToDom(samlResponse: InputStream): Document = ???

  /**
    * Reads the parsed saml response and checks the issuer against the filter config
    * and determines what version of the saml api we are using.
    *
    * @param document the parsed saml response
    * @return 1 or 2 as appropriate
    * @throws SamlPolicyException should it have problems finding the issuer
    */
  def determineVersion(document: Document): Int = ???

  /**
    * Determines whether or not the response follows the rules that are required of the saml response.
    * Additionally returns the issuer for the contained assertions.
    *
    * @param document the parsed saml response
    * @return the issuer for the embedded assertions
    * @throws SamlPolicyException if response is invalid
    */
  def validateResponseAndGetIssuer(document: Document): String = ???

  /**
    * Retrieves the policy from the configured endpoint. The caching will be handled elsewhere.
    * There is a possiblity that this method will have to get split into two calls is we end up needing the raw policy for the os response mangling.
    * I hope not, because that poops on what i'm trying to do with the cache at the moment.
    *
    * @param issuer the issuer
    * @return the compiled xslt that represents the policy
    * @throws SamlPolicyException for so many reasons
    */
  def getPolicy(issuer: String): XsltExecutable = ???

  /**
    * Applies the policy to the saml response.
    *
    * @param document the parsed saml response
    * @param policy the xslt translation
    * @return the translated document
    * @throws SamlPolicyException if the translation fails
    */
  def translateResponse(document: Document, policy: XsltExecutable): Document = ???

  /**
    * Signs the saml response.
    *
    * @param document the translated saml response
    * @return a signed saml response
    * @throws SamlPolicyException if the signing fails
    */
  def signResponse(document: Document): Document = ???

  /**
    * Converts the saml response document into a servlet input stream for passing down the filter chain.
    *
    * @param document the final saml response
    * @return the sinput stream that can be wrapped and passed along
    */
  def convertDocumentToStream(document: Document): ServletInputStream = ???

  /**
    * This is super vague because we don't know what it looks like yet.
    *
    * @return
    */
  def doResponseStuff() = ???

  /**
    * Evict from the cache as appropriate.
    *
    * @param atomEntry A {@link String} representation of an Atom entry. Note that Atom entries are XML elements,
    */
  override def onNewAtomEntry(atomEntry: String): Unit = ???

  /**
    * I suspect we don't care, but i could be wrong.
    *
    * @param event A value representing the new lifecycle stage of the system associated with the Feed that this
    */
  override def onLifecycleEvent(event: LifecycleEvents): Unit = ???

  /**
    * Stores the configuration and marks the filter as initialized.
    * I'm going to have to initialize the cache and atom feed listener here, but it's not in the critical path for the moment.
    *
    * @param newConfiguration
    */
  override def configurationUpdated(newConfiguration: SamlPolicyConfig): Unit = {
    val requestedFeedId = Option(newConfiguration.getPolicyAcquisition.getCache.getAtomFeedId)
    if (feedId.nonEmpty && (requestedFeedId != Option(configuration.getPolicyAcquisition.getCache.getAtomFeedId))) {
      atomFeedService.unregisterListener(feedId.get)
      feedId = None
    }

    if (feedId.isEmpty && requestedFeedId.nonEmpty) {
      feedId = Option(atomFeedService.registerListener(requestedFeedId.get, this))
    }

    super.configurationUpdated(newConfiguration)
  }
}

case class SamlPolicyException(statusCode: Int, message: String, cause: Throwable) extends Exception(message, cause) {
  def this(statusCode: Int, message: String) = this(statusCode, message, null)
}
