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

package org.openrepose.filters.uristripper

import java.net.{URI, URL}
import javax.inject.{Named, Inject}
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.servlet.http.{ResponseMode, HttpServletResponseWrapper, HttpServletRequestWrapper}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.uristripper.config.UriStripperConfig

import scala.collection.mutable
import scala.util.{Success, Failure, Try}

@Named
class UriStripperFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[UriStripperConfig] with LazyLogging {

  import UriStripperFilter._

  private var configurationFileName: String = DefaultConfigFileName
  private var initialized = false
  private var config: UriStripperConfig = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("URI Stripper filter initializing...")
    configurationFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfigFileName)

    logger.info(s"Initializing URI Stripper Filter using config $configurationFileName")
    val xsdUrl: URL = getClass.getResource(SchemaFileName)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFileName, xsdUrl, this, classOf[UriStripperConfig])

    logger.trace("URI Stripper filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val wrappedResponse = new HttpServletResponseWrapper(
        servletResponse.asInstanceOf[HttpServletResponse], ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val uriTokens = splitUriIntoTokens(wrappedRequest.getRequestURI)
      var previousToken: Option[String] = None
      var nextToken: Option[String] = None
      var token: Option[String] = None

      if (uriTokens.length > config.getTokenIndex) {
        if (config.getTokenIndex != 0) {
          previousToken = Option(uriTokens(config.getTokenIndex - 1)).filterNot(_.trim.isEmpty)
        }

        if (uriTokens.length > config.getTokenIndex + 1) {
          nextToken = Option(uriTokens(config.getTokenIndex + 1)).filterNot(_.trim.isEmpty)
        }

        token = Some(uriTokens.remove(config.getTokenIndex))
        wrappedRequest.setRequestURI(joinTokensIntoUri(uriTokens))
      }

      filterChain.doFilter(wrappedRequest, wrappedResponse)

      val originalLocation = wrappedResponse.getHeader(CommonHttpHeader.LOCATION.toString)
      if (config.isRewriteLocation && token.isDefined && !Option(originalLocation).forall(_.trim.isEmpty) && (previousToken.isDefined || nextToken.isDefined)) {

        if (originalLocation.contains(previousToken.getOrElse("") + UriDelimiter + token.get + UriDelimiter + nextToken.getOrElse(""))) {
          logger.debug("Stripped token already present in Location Header")
        } else {
          Try(new URI(originalLocation)) match {
            case Failure(exception) =>
              logger.warn("Unable to parse Location header. Location header is malformed URI", exception)
            case Success(uri) =>
              val locationTokens = splitUriIntoTokens(uri.getPath)

              // figure out where to add the token if we can
              ((previousToken.filter(locationTokens.contains), nextToken.filter(locationTokens.contains)) match {
                case (Some(foundToken), _) => Some(locationTokens.indexOf(foundToken) + 1)
                case (_, Some(foundToken)) => Some(locationTokens.indexOf(foundToken))
                case _ => None
              }).foreach(locationTokens.insert(_, token.get))

              val preUri = Option(uri.getScheme).map(_ + "://" + uri.getHost + (if (uri.getPort != -1) ":" + uri.getPort else ""))
              val postUri = Option(uri.getQuery).filterNot(_.trim.isEmpty).map(QueryParamIndicator +)
              wrappedResponse.replaceHeader(
                CommonHttpHeader.LOCATION.toString,
                preUri.getOrElse("") + joinTokensIntoUri(locationTokens) + postUri.getOrElse(""))
          }
        }
      }

      wrappedResponse.commitToResponse()
    }
  }

  private def splitUriIntoTokens(uri: String): mutable.Buffer[String] =
    StringUriUtilities.formatUriNoLead(uri).split(UriDelimiter).toBuffer

  private def joinTokensIntoUri(tokens: mutable.Buffer[String]): String =
    StringUriUtilities.formatUri(tokens.mkString(UriDelimiter))

  override def destroy(): Unit = {
    logger.trace("URI Stripper filter destroying...")
    configurationService.unsubscribeFrom(configurationFileName, this.asInstanceOf[UpdateListener[_]])
    logger.trace("URI Stripper filter destroyed.")
  }

  override def configurationUpdated(uriStripperConfig: UriStripperConfig): Unit = {
    config = uriStripperConfig
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object UriStripperFilter {
  private final val DefaultConfigFileName = "uri-stripper.cfg.xml"
  private final val SchemaFileName = "/META-INF/schema/config/uri-stripper.xsd"

  private final val UriDelimiter = "/"
  private final val QueryParamIndicator = "?"
}