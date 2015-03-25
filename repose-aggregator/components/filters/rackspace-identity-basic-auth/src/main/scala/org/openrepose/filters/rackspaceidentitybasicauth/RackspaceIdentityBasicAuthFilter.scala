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
package org.openrepose.filters.rackspaceidentitybasicauth

import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.{Calendar, GregorianCalendar}
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.HttpDate
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.{FilterDirectorImpl, FilterLogicHandlerDelegate}
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.rackspaceidentitybasicauth.config.RackspaceIdentityBasicAuthConfig
import org.springframework.http.HttpHeaders

import scala.collection.JavaConverters._
import scala.io.Source
import scala.xml.XML

@Named
class RackspaceIdentityBasicAuthFilter @Inject() (configurationService: ConfigurationService,
                                                  akkaServiceClient : AkkaServiceClient,
                                                  datastoreService : DatastoreService)
  extends AbstractFilterLogicHandler
  with Filter
  with UpdateListener[RackspaceIdentityBasicAuthConfig]
  with HttpDelegationManager
  with BasicAuthUtils
  with LazyLogging {

  private final val DEFAULT_CONFIG = "rackspace-identity-basic-auth.cfg.xml"

  private var initialized = false
  private var config: String = _
  private final val TOKEN_KEY_PREFIX = "TOKEN:"
  private final val X_AUTH_TOKEN = "X-Auth-Token"
  private var identityServiceUri: String = _
  private var tokenCacheTtlMillis: Int = _
  private var delegationWithQuality: Option[Double] = _
  private val datastore = datastoreService.getDefaultDatastore

  case class TokenCreationInfo(responseCode: Int, userId: Option[String], userName: String, retry: String)

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + config)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/rackspace-identity-basic-auth.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      this,
      classOf[RackspaceIdentityBasicAuthConfig]
    )
    logger.warn("WARNING: This filter cannot be used alone, it requires an AuthFilter after it.")
  }

  override def configurationUpdated(config: RackspaceIdentityBasicAuthConfig) {
    identityServiceUri = config.getRackspaceIdentityServiceUri
    tokenCacheTtlMillis = config.getTokenCacheTimeoutMillis
    delegationWithQuality = Option(config.getDelegating).map(_.getQuality)
    initialized = true
  }

  override def isInitialized = {
    initialized
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(this)
  }

  private def withEncodedCredentials(request: HttpServletRequest)(f: String => Unit): Unit = {
    Option(request.getHeaders(HttpHeaders.AUTHORIZATION)).map { authHeader =>
      val authMethodBasicHeaders = getBasicAuthHeaders(authHeader, "Basic")
      if (authMethodBasicHeaders.nonEmpty) {
        val firstHeader = authMethodBasicHeaders.next()
        f(firstHeader.replace("Basic ", ""))
      }
    }
  }

  override def handleRequest(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    logger.debug("Handling HTTP Request")
    val filterDirector: FilterDirector = new FilterDirectorImpl()

    def delegateOrElse(responseCode: Int, message: String)(f: => Any) = {
      delegationWithQuality match {
        case Some(quality) =>
          buildDelegationHeaders(responseCode, "Rackspace Identity Basic Auth", message, quality) foreach { case (key, values) =>
            filterDirector.requestHeaderManager.appendHeader(key, values: _*)
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
          }
        case None =>
          f
      }
    }

    def processFailedToken(code: Int, userName: String, retry: String, encodedCredentials: String): Unit = {
      code match {
        case (HttpServletResponse.SC_UNAUTHORIZED) =>
          delegateOrElse(HttpServletResponse.SC_UNAUTHORIZED, s"Failed to authenticate user: $userName") {
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED) // (401)
            filterDirector.responseHeaderManager().appendHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"RAX-KEY\"")
            datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
          }
        case (HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE | FilterDirector.SC_TOO_MANY_REQUESTS) => // (413 | 429)
          delegateOrElse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Rate limited by identity service") {
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE) // (503)
            filterDirector.responseHeaderManager().appendHeader(HttpHeaders.RETRY_AFTER, retry)
          }
        case (_) =>
          delegateOrElse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed with internal server error") {
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR) // (500)
          }
      }
      if (delegationWithQuality.isEmpty) filterDirector.setFilterAction(FilterAction.RETURN)
    }

    def getUserToken(authValue: String): TokenCreationInfo = {
      val (userName, apiKey) = extractCredentials(authValue)

      def createAuthRequest(encoded: String) = {
        // Base64 Decode and split the userName/apiKey
        // Scala's standard XML syntax does not support the XML declaration w/o a lot of hoops
        //<?xml version="1.0" encoding="UTF-8"?>
        <auth xmlns="http://docs.openstack.org/identity/api/v2.0">
          <apiKeyCredentials
          xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
          username={userName}
          apiKey={apiKey}/>
        </auth>
      }
      // Request a User Token based on the extracted User Name/API Key.
      val authTokenResponse = Option(akkaServiceClient.post(authValue,
        identityServiceUri,
        Map[String, String]().asJava,
        createAuthRequest(authValue).toString(),
        MediaType.APPLICATION_XML_TYPE))

      authTokenResponse.map { tokenResponse =>
        val statusCode = tokenResponse.getStatus
        if (statusCode == HttpServletResponse.SC_OK) {
          val xmlString = XML.loadString(Source.fromInputStream(tokenResponse.getData).mkString)
          val idString = (xmlString \\ "access" \ "token" \ "@id").text
          TokenCreationInfo(statusCode, Option(idString), userName, "0")
        } else if (statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE | statusCode == FilterDirector.SC_TOO_MANY_REQUESTS) { // (413 | 429)
        val retryHeaders = tokenResponse.getHeaders.filter { header => header.getName.equals(HttpHeaders.RETRY_AFTER)}
          if (retryHeaders.isEmpty) {
            logger.info(s"Missing ${HttpHeaders.RETRY_AFTER} header on Auth Response status code: $statusCode")
            val retryCalendar = new GregorianCalendar()
            retryCalendar.add(Calendar.SECOND, 5)
            val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
            TokenCreationInfo(statusCode, None, userName, retryString)
          } else {
            TokenCreationInfo(statusCode, None, userName, retryHeaders.head.getValue)
          }
        } else {
          TokenCreationInfo(statusCode, None, userName, "0")
        }
      } getOrElse {
        TokenCreationInfo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, None, userName, "0")
      }
    }

    def processNoCachedToken(encodedCredentials: String): Unit = {
      // request a token
      getUserToken(encodedCredentials) match {
        case TokenCreationInfo(code, Some(token), userName, _) =>
          val tokenStr = token.toString
          if (tokenCacheTtlMillis > 0) {
            datastore.put(TOKEN_KEY_PREFIX + encodedCredentials, tokenStr, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
          }
          filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, tokenStr)
        case TokenCreationInfo(code, _, userName, retry) =>
          processFailedToken(code, userName, retry, encodedCredentials)
      }
    }

    // We need to process the Response unless a couple of specific conditions occur.
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    if (!httpServletRequest.getHeaderNames.asScala.toList.contains(X_AUTH_TOKEN)) {
      withEncodedCredentials(httpServletRequest) { encodedCredentials =>
        Option(datastore.get(TOKEN_KEY_PREFIX + encodedCredentials)) match {
          case Some(token) =>
            val tokenString = token.toString
            filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, tokenString)
          case None =>
            processNoCachedToken(encodedCredentials)
        }
      }
    }
    filterDirector
  }

  override def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    logger.debug("Handling HTTP Response. Incoming status code: " + httpServletResponse.getStatus)
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    if (httpServletResponse.getStatus == HttpServletResponse.SC_UNAUTHORIZED ||
      httpServletResponse.getStatus == HttpServletResponse.SC_FORBIDDEN) {
      filterDirector.responseHeaderManager().appendHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"RAX-KEY\"")
      withEncodedCredentials(httpServletRequest) { encodedCredentials =>
        datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
      }
    }
    logger.debug("Rackspace Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatusCode)
    filterDirector
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])
  }
}
