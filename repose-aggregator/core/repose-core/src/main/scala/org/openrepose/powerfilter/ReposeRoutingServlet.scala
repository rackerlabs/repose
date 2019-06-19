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
package org.openrepose.powerfilter

import java.net.{MalformedURLException, URL}
import java.time.Duration
import java.util.Optional
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpUriRequest}
import org.apache.http.client.utils.URIBuilder
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.http.{CommonHttpHeader, CommonRequestAttributes}
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.commons.utils.logging.HttpLoggingContextHelper
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestUtil, HttpServletRequestWrapper, RouteDestination}
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.config._
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.openrepose.nodeservice.httpcomponent.{HttpComponentRequestProcessor, HttpComponentResponseProcessor}
import org.openrepose.nodeservice.response.LocationHeaderBuilder
import org.openrepose.powerfilter.ReposeRoutingServlet._
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConverters._
import scala.util.Try

@Named("reposeRoutingServlet")
class ReposeRoutingServlet @Inject()(@Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVersion: String,
                                     @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                     configurationService: ConfigurationService,
                                     containerConfigurationService: ContainerConfigurationService,
                                     httpClientService: HttpClientService,
                                     optMetricsService: Optional[MetricsService]
                                    ) extends HttpServlet with UpdateListener[SystemModel] with StrictLogging {

  private val metricsService: Option[MetricsService] = Option(optMetricsService.orElse(null))

  private var httpClient: HttpClientServiceClient = _
  private var localNode: Node = _
  private var defaultDestination: Destination = _
  private var destinations: Map[String, Destination] = Map.empty
  private var rewriteHostHeader: Boolean = _
  private var initialized = false

  override def init(): Unit = {
    logger.info("{} -- Reticulating Splines - Initializing Repose Routing Servlet", nodeId)
    configurationService.subscribeTo(
      SystemModelConfigurationFilename,
      getClass.getResource("/META-INF/schema/system-model/system-model.xsd"),
      this,
      classOf[SystemModel]
    )
    httpClient = httpClientService.getDefaultClient
    logger.trace("initialized.")
  }

  override def destroy(): Unit = {
    logger.trace("destroying ...")
    configurationService.unsubscribeFrom(SystemModelConfigurationFilename, this)
    logger.info("{} -- Obfuscated Quigley Matrix - Destroyed Repose Routing Servlet", nodeId)
  }

  override def configurationUpdated(configurationObject: SystemModel): Unit = {
    logger.trace("received a configuration update")

    val interrogator = new SystemModelInterrogator(nodeId)
    val node = interrogator.getNode(configurationObject)

    // The existence of the Spring context in which this object exists implies
    // the existence of a local node, so an explicit check is not necessary.
    localNode = node.get()

    defaultDestination = interrogator.getDefaultDestination(configurationObject).get()
    destinations = configurationObject.getDestinations.getEndpoint.asScala
      .map(it => it.getId -> it)
      .toMap
    rewriteHostHeader = configurationObject.isRewriteHostHeader

    //Build a list of the destinations in this cluster, just so we know what we're doing
    val destStrings = destinations
      .map { case (_, endpoint) => s"${endpoint.getId}-${endpoint.getHostname}" }
    logger.debug("{} - Destinations for this router: {}", nodeId, destStrings)

    initialized = true
  }

  override def isInitialized: Boolean = {
    initialized
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    if (!initialized) {
      logger.error(NotInitializedMessage)
      trySendError(resp, SC_INTERNAL_SERVER_ERROR, FailedToRouteMessage)
    } else {
      logger.trace("processing request...")

      val wrappedReq = new HttpServletRequestWrapper(req)

      getRoute(wrappedReq).flatMap { route =>
        getDestination(route).flatMap { destination =>
          getTarget(route, destination).flatMap { target =>
            // Fix the Via and X-Forwarded-For headers on the request
            setVia(wrappedReq)
            setXForwardedFor(wrappedReq)

            // Forward the request to the origin server (wrapping metrics around the action)
            preProxyMetrics(destination)
            val startTime = System.currentTimeMillis
            processRequest(target, wrappedReq).flatMap(proxyRequest).map { clientResponse =>
              processResponse(clientResponse, resp)

              val stopTime = System.currentTimeMillis
              val timeElapsed = stopTime - startTime
              postProxyMetrics(timeElapsed, resp, destination, target.url)
              updateLoggingContext(wrappedReq, clientResponse, timeElapsed)

              // Fix the Location header on the response
              fixLocationHeader(
                wrappedReq,
                resp,
                route,
                target.url.getPath,
                destination.getRootPath)
            }
          }
        }
      }.recover {
        case e if ExceptionUtils.getRootCause(e).isInstanceOf[ReadLimitReachedException] =>
          logger.error(RequestContentErrorMessage, e)
          trySendError(resp, SC_REQUEST_ENTITY_TOO_LARGE, RequestContentErrorMessage)
        case e: OriginServiceCommunicationException =>
          logger.error(OriginServiceErrorMessage, e)
          trySendError(resp, SC_BAD_GATEWAY, OriginServiceErrorMessage)
        case e =>
          logger.error(FailedToRouteMessage, e)
          trySendError(resp, SC_INTERNAL_SERVER_ERROR, FailedToRouteMessage)
      }

      logger.trace("returning response...")
    }
  }

  def trySendError(servletResponse: HttpServletResponse, sc: Int, msg: String): Unit = {
    if (!servletResponse.isCommitted) {
      servletResponse.sendError(sc, msg)
    } else {
      logger.error(s"Failed to Send Error $sc with message '$msg' due to response already being committed")
    }
  }

  def getRoute(servletRequest: HttpServletRequest): Try[RouteDestination] = Try {
    val attributeRoutes = Option(servletRequest.getAttribute(CommonRequestAttributes.DESTINATIONS)) match {
      case None => Seq.empty
      case Some(destinations: java.util.List[RouteDestination]) => destinations.asScala
      case Some(destinations: Seq[RouteDestination]) => destinations
      case _ => throw new Exception("Route could not be determined from the servlet request -- the destinations attribute was not a supported type")
    }
    val defaultRoute = new RouteDestination(defaultDestination.getId, servletRequest.getRequestURI, -1)

    (attributeRoutes :+ defaultRoute)
      .sortWith(_.compareTo(_) > 0) // Highest Quality first
      .head
  }

  def getDestination(route: RouteDestination): Try[Destination] = Try {
    destinations.getOrElse(route.getDestinationId, {
      logger.warn("Invalid routing destination specified: {}", route.getDestinationId)
      throw new Exception("Invalid routing destination specified")
    })
  }

  def getTarget(route: RouteDestination, destination: Destination): Try[Target] = Try {
    val uriBuilder = new URIBuilder()
      .setScheme(destination.getProtocol)
      .setHost(destination.getHostname)
      .setPath(StringUriUtilities.concatUris(destination.getRootPath, route.getUri))
    Option(destination.getPort).filterNot(_ == 0).foreach(uriBuilder.setPort)
    Target(uriBuilder.build().toURL, destination.getChunkedEncoding)
  }

  def preProxyMetrics(destination: Destination): Unit = {
    metricsService.foreach { metSer =>
      metSer.getRegistry.meter(
        MetricRegistry.name("org.openrepose.core.RequestDestination", destination.getId)
      ).mark()
    }
  }

  def postProxyMetrics(timeElapsed: Long,
                       servletResponse: HttpServletResponse,
                       destination: Destination,
                       targetUrl: URL): Unit = {
    // track response code for endpoint & across all endpoints
    val endpoint = getEndpoint(destination, targetUrl)
    val servletResponseStatus = servletResponse.getStatus
    metricsService.foreach { metSer =>
      markResponseCodeHelper(metSer.getRegistry, servletResponseStatus, timeElapsed, endpoint)
    }
  }

  def updateLoggingContext(request: HttpServletRequest, clientResponse: CloseableHttpResponse, timeElapsed: Long): Unit = {
    Option(HttpLoggingContextHelper.extractFromRequest(request)).foreach { loggingContext =>
      loggingContext.setInboundResponseProtocol(clientResponse.getProtocolVersion.toString)
      logger.trace("Added the inbound response protocol to the HTTP Logging Service context {}", s"${loggingContext.hashCode()}")

      loggingContext.setTimeInOriginService(Duration.ofMillis(timeElapsed))
      logger.trace("Added the time elapsed to the HTTP Logging Service context {}", s"${loggingContext.hashCode()}")
    }
  }

  def processRequest(target: Target, servletRequest: HttpServletRequest): Try[HttpUriRequest] = Try {
    // Translate the servlet request to an HTTP client request
    HttpComponentRequestProcessor.process(
      servletRequest,
      target.url.toURI,
      rewriteHostHeader,
      target.chunkedEncoding)
  }

  def proxyRequest(clientRequest: HttpUriRequest): Try[CloseableHttpResponse] = Try {
    // Execute the HTTP client request
    logger.debug("Forwarding the request to: {}", clientRequest.getURI)
    val cacheContext = CachingHttpClientContext.create()
      .setUseCache(false)
    try {
      httpClient.execute(clientRequest, cacheContext)
    } catch {
      case e: Throwable => throw OriginServiceCommunicationException(e)
    }
  }

  def processResponse(clientResponse: CloseableHttpResponse, servletResponse: HttpServletResponse): Try[Unit] = Try {
    // Translate the HTTP client response to a servlet response
    HttpComponentResponseProcessor.process(clientResponse, servletResponse)
  }

  def setVia(request: HttpServletRequestWrapper): Unit = {
    val builder = new StringBuilder
    builder.append(HttpServletRequestUtil.getProtocolVersion(request))
    val requestVia = containerConfigurationService.getRequestVia
    builder.append(" ")
    if (requestVia.isPresent) {
      builder.append(requestVia.get)
    } else {
      builder.append(localNode.getHostname).append(":").append(request.getLocalPort)
    }
    builder.append(" (Repose/").append(reposeVersion).append(")")
    request.addHeader(CommonHttpHeader.VIA, builder.toString)
  }

  def setXForwardedFor(request: HttpServletRequestWrapper): Unit = {
    request.addHeader(CommonHttpHeader.X_FORWARDED_FOR, request.getRemoteAddr)
  }

  def getEndpoint(dest: Destination, targetUrl: URL): String = {
    val sb = new StringBuilder()
    sb.append(targetUrl.getHost)
    if (targetUrl.getPort != -1) {
      sb.append(":").append(targetUrl.getPort)
    }
    sb.append(dest.getRootPath)
    sb.toString
  }

  def markResponseCodeHelper(metricRegistry: MetricRegistry, responseCode: Int, lengthInMillis: Long, endpoint: String): Unit = {
    if (100 <= responseCode && responseCode < 600) {
      Seq(endpoint, AllEndpoints).foreach { location =>
        val statusCodeClass = "%dXX".format(responseCode / 100)
        metricRegistry.meter(
          MetricRegistry.name("org.openrepose.core.ResponseCode", location, statusCodeClass)
        ).mark()
        metricRegistry.timer(
          MetricRegistry.name("org.openrepose.core.ResponseTime", location, statusCodeClass)
        ).update(lengthInMillis, TimeUnit.MILLISECONDS)
        if (responseCode == SC_REQUEST_TIMEOUT) {
          metricRegistry.meter(
            MetricRegistry.name("org.openrepose.core.RequestTimeout.TimeoutToOrigin", location)
          ).mark()
        }
      }
    } else {
      logger.error(s"$endpoint Encountered invalid response code: $responseCode")
    }
  }

  def fixLocationHeader(originalRequest: HttpServletRequest, response: HttpServletResponse, destination: RouteDestination, destinationLocationUri: String, proxiedRootContext: String): Unit = {
    var destinationUri = Option(destinationLocationUri).map(_.split("\\?")(0)).getOrElse("")
    Try(LocationHeaderBuilder.setLocationHeader(originalRequest, response, destinationUri, destination.getContextRemoved, proxiedRootContext)).recover {
      case ex: MalformedURLException =>
        logger.warn("Invalid URL in location header processing", ex)
    }
  }
}

object ReposeRoutingServlet {
  final val SystemModelConfigurationFilename = "system-model.cfg.xml"
  final val NotInitializedMessage = "The ReposeRoutingServlet has not yet been initialized"
  final val FailedToRouteMessage = "Failed to route the request to the origin service"
  final val OriginServiceErrorMessage = "Error communicating with origin service"
  final val RequestContentErrorMessage = "Error reading request content"
  private final val AllEndpoints = "All Endpoints"

  case class Target(url: URL, chunkedEncoding: ChunkedEncoding)

  case class OriginServiceCommunicationException(cause: Throwable) extends Exception(cause: Throwable)

}
