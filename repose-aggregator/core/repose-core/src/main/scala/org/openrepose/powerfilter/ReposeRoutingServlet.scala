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

import java.io.IOException
import java.net.{MalformedURLException, URL}
import java.util.Optional
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.URIBuilder
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.http.{CommonHttpHeader, CommonRequestAttributes}
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestUtil, HttpServletRequestWrapper, RouteDestination}
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.routing.robin.Clusters
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
                                     @Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                     @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                     configurationService: ConfigurationService,
                                     containerConfigurationService: ContainerConfigurationService,
                                     httpClientService: HttpClientService,
                                     optMetricsService: Optional[MetricsService]
                                    ) extends HttpServlet with UpdateListener[SystemModel] with StrictLogging {

  private val metricsService: Option[MetricsService] = Option(optMetricsService.orElse(null))

  private var httpClient: HttpClientServiceClient = _
  private var localCluster: ReposeCluster = _
  private var localNode: Node = _
  private var defaultDestination: Destination = _
  private var destinations: Map[String, Destination] = Map.empty
  private var domains: Clusters = _ // TODO: Remove this when routing to a cluster is removed.
  private var rewriteHostHeader: Boolean = _
  private var initialized = false

  override def init(): Unit = {
    logger.info("{}:{} -- Reticulating Splines - Initializing Repose Routing Servlet", clusterId, nodeId)
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
    logger.info("{}:{} -- Obfuscated Quigley Matrix - Destroyed Repose Routing Servlet", clusterId, nodeId)
  }

  override def configurationUpdated(configurationObject: SystemModel): Unit = {
    logger.trace("received a configuration update")

    val interrogator = new SystemModelInterrogator(clusterId, nodeId)
    val cluster = interrogator.getLocalCluster(configurationObject)
    val node = interrogator.getLocalNode(configurationObject)

    if (cluster.isPresent && node.isPresent) {
      localCluster = cluster.get()
      localNode = node.get()

      defaultDestination = interrogator.getDefaultDestination(configurationObject).get()
      destinations = (localCluster.getDestinations.getEndpoint.asScala ++ localCluster.getDestinations.getTarget.asScala)
          .map(it => it.getId -> it)
          .toMap
      domains = new Clusters(configurationObject)
      rewriteHostHeader = localCluster.isRewriteHostHeader

      val clusterId: String = localCluster.getId
      //Build a list of the nodes in this cluster, just so we know what we're doing
      val clusterNodes = localCluster.getNodes.getNode.asScala.map(n => s"${n.getId}-${n.getHostname}")
      logger.debug("{}:{} - Nodes for this router: {}", clusterId, nodeId, clusterNodes)
      val destStrings = destinations
        .filter { case (_, dest) => dest.isInstanceOf[DestinationEndpoint] }
        .map { case (_, endpoint) => s"${endpoint.getId}-${endpoint.asInstanceOf[DestinationEndpoint].getHostname}" }
      logger.debug("{}:{} - Destinations for this router: {}", clusterId, nodeId, destStrings)

      initialized = true
    } else {
      logger.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml")
    }
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

      val servletRequest = new HttpServletRequestWrapper(req)

      getRoute(servletRequest).flatMap { route =>
        getDestination(route).flatMap { destination =>
          getTarget(route, destination).map { target =>
            // Fix the Via and X-Forwarded-For headers on the request
            setVia(servletRequest)
            setXForwardedFor(servletRequest)

            // Forward the request to the origin server (wrapping metrics around the action)
            preProxyMetrics(destination)
            val startTime = System.currentTimeMillis
            proxyRequest(target, servletRequest, resp)
            val stopTime = System.currentTimeMillis
            postProxyMetrics(stopTime - startTime, resp, destination, target.url)

            // Fix the Location header on the response
            fixLocationHeader(
              servletRequest,
              resp,
              route,
              target.url.getPath,
              destination.getRootPath)
          }
        }
      }.recover {
        case e: IOException if ExceptionUtils.getRootCause(e).isInstanceOf[ReadLimitReachedException] =>
          logger.error(RequestContentErrorMessage, e)
          trySendError(resp, SC_REQUEST_ENTITY_TOO_LARGE, RequestContentErrorMessage)
        case e: OriginServiceCommunicationException =>
          logger.error(OriginServiceErrorMessage, e)
          trySendError(resp, SC_SERVICE_UNAVAILABLE, OriginServiceErrorMessage)
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
      logger.warn("Invalid routing destination specified: {} for cluster: {}", route.getDestinationId, localCluster.getId)
      throw new Exception("Invalid routing destination specified")
    })
  }

  def getTarget(route: RouteDestination, destination: Destination): Try[Target] = Try {
    destination match {
      case endpoint: DestinationEndpoint =>
        val uriBuilder = new URIBuilder()
          .setScheme(endpoint.getProtocol)
          .setHost(endpoint.getHostname)
          .setPath(StringUriUtilities.concatUris(endpoint.getRootPath, route.getUri))
        Option(endpoint.getPort).filterNot(_ == 0).foreach(uriBuilder.setPort)
        Target(uriBuilder.build().toURL, endpoint.getChunkedEncoding)
      case cluster: DestinationCluster =>
        // todo: remove this when routing to a cluster is removed.
        Option(domains.getDomain(cluster.getCluster.getId)) match {
          case Some(clusterWrapper) =>
            val routableNode = clusterWrapper.getNextNode
            val port = if (HttpsProtocol.equalsIgnoreCase(cluster.getProtocol)) routableNode.getHttpsPort else routableNode.getHttpPort
            val targetUrl = new URL(cluster.getProtocol, routableNode.getHostname, port, StringUriUtilities.concatUris(cluster.getRootPath, route.getUri))
            Target(targetUrl, cluster.getChunkedEncoding)
          case None =>
            logger.warn("No routable node for domain: {}", cluster.getId)
            throw new Exception("No routable node for domain: " + cluster.getId)
        }
      case _ =>
        // todo: remove this when routing to a cluster is removed (it would only ever occur due to a development issue anyway).
        logger.error(s"Unknown destination type: ${destination.getClass.getName} -- please notify the Repose team")
        throw new Exception("Unknown destination type")
    }
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

  def proxyRequest(target: Target, servletRequest: HttpServletRequest, servletResponse: HttpServletResponse): Try[Unit] = Try {
    // Translate the servlet request to an HTTP client request
    val processedClientRequest = HttpComponentRequestProcessor.process(
      servletRequest,
      target.url.toURI,
      rewriteHostHeader,
      target.chunkedEncoding)

    // Execute the HTTP client request
    logger.debug("Forwarding the request to: {}", processedClientRequest.getURI)
    val clientResponse = try {
      httpClient.execute(processedClientRequest)
    } catch {
      case e: Throwable => throw OriginServiceCommunicationException(e)
    }

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
  private final val HttpsProtocol = "https"

  case class Target(url: URL, chunkedEncoding: ChunkedEncoding)

  case class OriginServiceCommunicationException(cause: Throwable) extends Exception(cause: Throwable)
}
