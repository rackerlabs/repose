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

import java.net.{URI, URL}
import java.util.regex.Pattern
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.google.common.net.InetAddresses
import com.typesafe.scalalogging.StrictLogging
import org.apache.http.client.utils.URIBuilder
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, CorsHttpHeader}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.cors.config.CorsConfig

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

@Named
class CorsFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[CorsConfig] with StrictLogging {

  import CorsFilter._

  private var configurationFile: String = _
  private var initialized = false
  private var allowedOrigins: Seq[Regex] = _
  private var allowedMethods: Seq[String] = _
  private var resources: Seq[Resource] = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("CORS filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfig)

    logger.info(s"Initializing CORS Filter using config $configurationFile")
    val xsdUrl: URL = getClass.getResource(SchemaFilename)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[CorsConfig])

    logger.trace("CORS filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpServletRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val httpServletResponse = new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse],
        ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      val requestType = determineRequestType(httpServletRequest)

      val validationResult = requestType match {
        case NonCorsRequest => Pass(Seq.empty)
        case InvalidCorsRequest(message) => BadRequest(message)
        case PreflightCorsRequest(origin, method) => validateCorsRequest(origin, method, httpServletRequest.getRequestURI)
        case ActualCorsRequest(origin) => validateCorsRequest(origin, httpServletRequest.getMethod, httpServletRequest.getRequestURI)
      }

      validationResult match {
        case Pass(validMethods) =>
          requestType match {
            case NonCorsRequest => filterChain.doFilter(httpServletRequest, httpServletResponse)
            case PreflightCorsRequest(origin, _) =>
              logger.trace("Allowing CORS Preflight request.")
              httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, true.toString)
              httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
              httpServletRequest.getSplittableHeaderScala(CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS) foreach {
                httpServletResponse.appendHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, _)
              }
              validMethods foreach {
                httpServletResponse.appendHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS, _)
              }
              httpServletResponse.setStatus(HttpServletResponse.SC_OK)
            case ActualCorsRequest(origin) =>
              logger.trace("Allowing CORS Actual request.")
              filterChain.doFilter(httpServletRequest, httpServletResponse)
              httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, true.toString)
              httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)

              getHeaderNamesToExpose(httpServletResponse) foreach {
                httpServletResponse.appendHeader(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS, _)
              }
            case _ => logger.error("The request was validated but was not a validatable request type. This should not " +
              "be possible. ValidationResult: '{}', RequestType: '{}'", validationResult, requestType)
          }
        case OriginNotAllowed(origin) =>
          logger.debug("CORS request rejected because origin '{}' is not allowed.", origin)
          httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN)
        case MethodNotAllowed(origin, method, resource) =>
          logger.debug("CORS request rejected because method '{}' is not allowed for resource '{}'.", method, resource)
          httpServletResponse.setHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
          httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN)
        case BadRequest(message) =>
          // TODO: update to httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, message)
          httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST)
          httpServletResponse.setOutput(null)
          httpServletResponse.setContentType(MediaType.TEXT_PLAIN)
          httpServletResponse.getOutputStream.print(message)
      }

      // always add the Vary header
      httpServletResponse.addHeader(HttpHeaders.VARY, CorsHttpHeader.ORIGIN)
      if (httpServletRequest.getMethod == HttpMethod.OPTIONS) {
        httpServletResponse.addHeader(HttpHeaders.VARY, CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS)
        httpServletResponse.addHeader(HttpHeaders.VARY, CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
      }

      httpServletResponse.commitToResponse()
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

  def determineRequestType(request: HttpServletRequestWrapper): RequestType = {
    val originHeader = request.getHeader(CorsHttpHeader.ORIGIN)
    val isOptionsRequest = request.getMethod == HttpMethod.OPTIONS
    val preflightRequestedMethod = request.getHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
    lazy val originUri = getOriginUri(originHeader)
    lazy val hostUri = getHostUri(request)

    (Option(originHeader), isOptionsRequest, Option(preflightRequestedMethod)) match {
      case (None, _, _) =>
        logger.trace("Request is Non-CORS request because it does not have an Origin header.")
        NonCorsRequest
      case (Some(origin), true, Some(requestedMethod)) =>
        logger.debug("Request is CORS Preflight request because it is an OPTIONS request with Origin and preflight headers.")
        PreflightCorsRequest(origin, requestedMethod)
      case (Some(_), _, _) if originUri.isFailure =>
        logger.debug("Request has a malformed Origin header and will be rejected.", originUri.failed.get)
        InvalidCorsRequest("Bad Origin header")
      case (Some(_), _, _) if originUri.get == hostUri =>
        logger.trace("Request is Non-CORS request because the Origin header matched the Host/X-Forwarded-Host header (same-origin).")
        NonCorsRequest
      case (Some(origin), _, _) =>
        logger.debug("Request is CORS Actual request because the Origin header did not match the Host/X-Forwarded-Host header.")
        ActualCorsRequest(origin)
    }
  }

  def validateCorsRequest(origin: String, method: String, requestUri: String): CorsValidationResult =
    (isOriginAllowed(origin), getValidMethodsForResource(requestUri)) match {
      case (true, validMethods) if validMethods.contains(method) => Pass(validMethods)
      case (false, _) => OriginNotAllowed(origin)
      case (true, _) => MethodNotAllowed(origin, method, requestUri)
    }

  def isOriginAllowed(requestOrigin: String): Boolean = allowedOrigins.exists(_.findFirstIn(requestOrigin).isDefined)

  def getValidMethodsForResource(path: String): Seq[String] = {
    allowedMethods ++ (resources.find(_.path.findFirstIn(path).isDefined) match {
      case Some(matchedResource) =>
        logger.trace("Matched path '{}' with configured resource '{}'.", path, matchedResource)
        matchedResource.methods
      case None =>
        logger.trace("Did not find a configured resource matching path '{}'.", path)
        Nil
    })
  }

  def getHostUri(request: HttpServletRequestWrapper): URI = {
    val maybeForwardedHost = request.getSplittableHeaderScala(CommonHttpHeader.X_FORWARDED_HOST).headOption
    val maybeForwardedHostUri = Try(maybeForwardedHost
      .map(forwardedHost => new URIBuilder(s"${request.getScheme}://$forwardedHost"))
      .map(uri => uri.setPort(normalizePort(uri.getPort, uri.getScheme)).setHost(normalizeHost(uri.getHost)).build()))
    lazy val hostUri = new URIBuilder("").setScheme(request.getScheme).setHost(normalizeHost(request.getServerName))
      .setPort(normalizePort(request.getServerPort, request.getScheme)).build()
    maybeForwardedHostUri match {
      case Success(None) =>
        // there was no X-Forwarded-Host header in the request; don't bother logging this since this is the standard case
        hostUri
      case Success(Some(forwardedHostUri)) =>
        logger.trace("Using X-Forwarded-Host header '{}' for host.", maybeForwardedHost.get)
        forwardedHostUri
      case Failure(exception) =>
        logger.trace("Unable to parse X-Forwarded-Host header '{}'. Defaulting to Host header.", maybeForwardedHost.get, exception)
        hostUri
    }
  }

  def getOriginUri(origin: String): Try[URI] = Try(new URIBuilder(origin))
    .map(originUri => originUri
      .setPort(normalizePort(originUri.getPort, originUri.getScheme))
      .setHost(normalizeHost(originUri.getHost))
      .build())

  def normalizeHost(host: String): String = {
    val hostReplaced = if (host.startsWith("[") && host.endsWith("]")) host.substring(1, host.length - 1) else host
    if (InetAddresses.isInetAddress(hostReplaced)) InetAddresses.toAddrString(InetAddresses.forString(hostReplaced)) else hostReplaced
  }

  def normalizePort(port: Int, scheme: String): Int = (port, scheme.toLowerCase) match {
    case (p, _) if p > 0 => p
    case (_, s) if s == "http" => 80
    case (_, s) if s == "https" => 443
    case _ => port
  }

  def getHeaderNamesToExpose(response: HttpServletResponseWrapper): List[String] =
    DefaultExposeHeaders.filterNot(response.containsHeader) ++ response.getHeaderNames.asScala
}

object CorsFilter {
  private final val DefaultConfig = "cors.cfg.xml"
  private final val SchemaFilename = "/META-INF/schema/config/cors-configuration.xsd"
  private final val DefaultExposeHeaders = List(HttpHeaders.CONTENT_LENGTH)

  sealed trait RequestType
  object NonCorsRequest extends RequestType
  case class PreflightCorsRequest(origin: String, requestedMethod: String) extends RequestType
  case class ActualCorsRequest(origin: String) extends RequestType
  case class InvalidCorsRequest(message: String) extends RequestType

  sealed trait CorsValidationResult
  case class Pass(validMethods: Seq[String]) extends CorsValidationResult
  case class OriginNotAllowed(origin: String) extends CorsValidationResult
  case class MethodNotAllowed(origin: String, method: String, resource: String) extends CorsValidationResult
  case class BadRequest(message: String) extends CorsValidationResult

  case class Resource(path: Regex, methods: Seq[String])
}
