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
package org.openrepose.filters.cors

import java.net.URL
import java.util.regex.Pattern
import javax.inject.{Named, Inject}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.HttpMethod

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{HeaderConstant, CommonHttpHeader, CorsHttpHeader}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.cors.config.CorsConfig

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.matching.Regex

@Named
class CorsFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[CorsConfig] with LazyLogging {

  import CorsFilter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var allowedOrigins: Iterable[Regex] = _
  private var allowedMethods: Iterable[String] = _
  private var resources: Iterable[Resource] = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("CORS filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing CORS Filter using config $configurationFile")
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[CorsConfig])

    logger.trace("CORS filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      doFilterGuarded(servletRequest, servletResponse, filterChain)
    }
  }

  private def doFilterGuarded(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]
    val isOptions = httpServletRequest.getMethod == HttpMethod.OPTIONS
    val origin = httpServletRequest.getHeader(CorsHttpHeader.ORIGIN)
    val requestMethodHeader = httpServletRequest.getHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
    lazy val validMethods = getValidMethodsForResource(httpServletRequest.getRequestURI)

    val requestType = (Option(origin), isOptions, Option(requestMethodHeader)) match {
        case (Some(_), true, Some(_)) => PreflightRequest
        case (Some(_), _, None) => ActualRequest
        case _ => NonCorsRequest
    }

    val requestedMethod = requestType match {
      case PreflightRequest => requestMethodHeader
      case _ => httpServletRequest.getMethod
    }

    val validationResult = requestType match {
      case NonCorsRequest => Pass
      case _ =>
        (isOriginAllowed(origin), validMethods.exists(requestedMethod == _)) match {
          case (true, true) => Pass
          case (false, _) => OriginNotAllowed
          case (true, false) => MethodNotAllowed
        }
    }

    validationResult match {
      case Pass =>
        requestType match {
          case NonCorsRequest => filterChain.doFilter(httpServletRequest, httpServletResponse)
          case PreflightRequest =>
            httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, true.toString)
            httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
            Option(httpServletRequest.getHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS)).foreach {
              httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, _)}
            validMethods.foreach {httpServletResponse.addHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS, _)}
            httpServletResponse.setStatus(HttpServletResponse.SC_OK)
          case ActualRequest =>
            filterChain.doFilter(httpServletRequest, httpServletResponse)
            httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, true.toString)
            httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)

            // clone the list of header names so we can add headers while we iterate through it
            (List.empty ++ httpServletResponse.getHeaderNames.asScala).foreach {
              httpServletResponse.addHeader(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS, _)
            }
        }
      case OriginNotAllowed =>
        httpServletResponse.setHeader(CorsHttpHeader.ORIGIN, "null")
        httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN)
      case MethodNotAllowed =>
        httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
        httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN)
    }

    // always add the Vary header
    httpServletResponse.addHeader(CommonHttpHeader.VARY, CorsHttpHeader.ORIGIN)
    if (isOptions) {
      httpServletResponse.addHeader(CommonHttpHeader.VARY, CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS)
      httpServletResponse.addHeader(CommonHttpHeader.VARY, CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
    }
  }

  override def destroy(): Unit = {
    logger.trace("CORS filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("CORS filter destroyed.")
  }

  override def configurationUpdated(config: CorsConfig): Unit = {
    allowedOrigins = config.getAllowedOrigins.getOrigin.asScala.map { origin =>
      if (origin.isRegex) origin.getValue.r else Pattern.quote(origin.getValue).r
    }

    allowedMethods = Option(config.getAllowedMethods).map(_.getMethod.asScala).getOrElse(List())

    resources = Option(config.getResources)
      .map(_.getResource.asScala).getOrElse(List())
      .map { configResource =>
      Resource(configResource.getPath.r,
        Option(configResource.getAllowedMethods).map(_.getMethod.asScala).getOrElse(List()))}

    initialized = true
  }

  override def isInitialized: Boolean = initialized

  def isOriginAllowed(requestOrigin: String): Boolean = allowedOrigins.exists(_.findFirstIn(requestOrigin).isDefined)

  def getValidMethodsForResource(path: String): Iterable[String] = {
    allowedMethods ++ (resources.find(_.path.findFirstIn(path).isDefined) match {
      case Some(matchedResource) =>
        logger.debug(s"Matched path $path with configured resource: $matchedResource")
        matchedResource.methods
      case None =>
        logger.debug(s"Did not find a configured resource matching path $path")
        Nil
    })
  }
}

object CorsFilter {
  private final val DEFAULT_CONFIG = "cors.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/cors-configuration.xsd"

  implicit def autoHeaderToString(hc: HeaderConstant): String = hc.toString

  sealed trait RequestType
  object NonCorsRequest extends RequestType
  object PreflightRequest extends RequestType
  object ActualRequest extends RequestType

  sealed trait CorsValidationResult
  object Pass extends CorsValidationResult
  object OriginNotAllowed extends CorsValidationResult
  object MethodNotAllowed extends CorsValidationResult

  case class Resource(path: Regex, methods: Iterable[String])
}