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
package org.openrepose.core

import java.io.IOException
import java.net.{MalformedURLException, URL}
import java.util.concurrent.atomic.AtomicReference

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletResponse.{SC_INTERNAL_SERVER_ERROR, SC_REQUEST_TIMEOUT}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import javax.servlet.{RequestDispatcher, ServletContext}
import org.apache.commons.lang3.StringUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, RouteDestination}
import org.openrepose.core.ReposeRoutingServlet._
import org.openrepose.core.domain.Port
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.ReportingService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.routing.robin.Clusters
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.config._
import org.openrepose.nodeservice.request.RequestHeaderService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

@Named
class ReposeRoutingServlet @Inject()(@Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                     @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                     configurationService: ConfigurationService,
                                     requestHeaderService: RequestHeaderService,
                                     responseHeaderService: ResponseHeaderService,
                                     reportingService: ReportingService,
                                     metricsService: Option[MetricsService]
                                    ) extends HttpServlet with UpdateListener[SystemModel] with StrictLogging {

  private val localCluster = new AtomicReference[Option[ReposeCluster]](None)
  private val localNode = new AtomicReference[Option[Node]](None)
  private val defaultDestination = new AtomicReference[Option[Destination]](None)
  private val destinations = new AtomicReference[Map[String, Destination]](Map.empty[String, Destination])
  private val domains = new AtomicReference[Option[Clusters]](None) // TODO: Remove this when routing to a cluster is removed.
  private var initialized = false

  override def init(): Unit = {
    logger.info("{}:{} -- Reticulating Splines - Initializing Repose Routing Servlet", clusterId, nodeId)
    configurationService.subscribeTo(
      SystemModelConfigurationFilename,
      getClass.getResource("/META-INF/schema/system-model/system-model.xsd"),
      this,
      classOf[SystemModel]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(SystemModelConfigurationFilename, this)
    logger.info("{}:{} -- Obfuscated Quigley Matrix - Destroying Repose Routing Servlet", clusterId, nodeId)
  }

  override def configurationUpdated(configurationObject: SystemModel): Unit = {
    //Set the current system model, and just update the nodes.
    val interrogator = new SystemModelInterrogator(clusterId, nodeId)
    localCluster.set(Option(interrogator.getLocalCluster(configurationObject).orElse(null)))
    localNode.set(Option(interrogator.getLocalNode(configurationObject).orElse(null)))
    defaultDestination.set(Option(interrogator.getDefaultDestination(configurationObject).orElse(null)))
    destinations.set(localCluster.get().map(_.getDestinations) match {
      case Some(dests) => getDestinations(dests.getEndpoint.asScala.toList) ++ getDestinations(dests.getTarget.asScala.toList)
      case None => Map.empty[String, Destination]
    })
    domains.set(Option(new Clusters(configurationObject)))

    if (logger.underlying.isDebugEnabled) {
      localCluster.get().foreach { cluster =>
        val clusterId: String = cluster.getId
        //Build a list of the nodes in this cluster, just so we know what we're doing
        val clusterNodes = cluster.getNodes.getNode.asScala.map(node => s"${node.getId}-${node.getHostname}")
        logger.debug("{}:{} - Nodes for this router: {}", clusterId, nodeId, clusterNodes)
        val destStrings = destinations.get()
          .filter{ case (_, dest) => dest.isInstanceOf[DestinationEndpoint]}
          .map{ case (_, endpoint) => s"${endpoint.getId}-${endpoint.asInstanceOf[DestinationEndpoint].getHostname}"}
        logger.debug("{}:{} - Destinations for this router: {}", clusterId, nodeId, destStrings)
      }
    }
    initialized = true

    def getDestinations(destList: List[Destination]): Map[String, Destination] = {
      destList.iterator.map(dest => dest.getId -> dest).toMap
    }
  }

  override def isInitialized: Boolean = {
    initialized
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    if (!initialized) {
      logger.error(NotInitializedMessage)
      trySendError()
    } else {
      logger.trace("{} processing request...", this.getClass.getSimpleName)
      try {
        doRoute(new HttpServletRequestWrapper(req), resp)
      } catch {
        case NonFatal(e) =>
          logger.error(FailedToRouteMessage, e)
          trySendError()
      }
      logger.trace("{} returning response...", this.getClass.getSimpleName)
    }

    def trySendError(): Unit = {
      if (!resp.isCommitted) {
        resp.sendError(SC_INTERNAL_SERVER_ERROR, FailedToRouteMessage)
      } else {
        logger.error("Failed to Send Error due to response already being committed")
      }
    }
  }

  def doRoute(servletRequest: HttpServletRequestWrapper, servletResponse: HttpServletResponse): Unit = {
    val attributeDestinations =
      Option(servletRequest.getAttribute(CommonRequestAttributes.DESTINATIONS))
        .getOrElse(List.empty[RouteDestination]).asInstanceOf[List[RouteDestination]]
    val defaultDestinations = {
      defaultDestination.get() match {
        case Some(defDest) =>
          List[RouteDestination] {
            new RouteDestination(defDest.getId, servletRequest.getRequestURI, -1)
          }
        case _ => List.empty[RouteDestination]
      }
    }
    (attributeDestinations ++ defaultDestinations)
      .sortWith(_.compareTo(_) > 0) // Highest Quality first
      .headOption
      .foreach { routeDest =>
        localNode.get() match {
          case Some(node) => doDestination(servletRequest, servletResponse, routeDest, node)
          case _ =>
            logger.error("Invalid local node definition")
            servletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
        }
      }
  }

  def doDestination(servletRequest: HttpServletRequestWrapper, servletResponse: HttpServletResponse, routeDest: RouteDestination, node: Node): Unit = {
    destinations.get().get(routeDest.getDestinationId) match {
      case Some(destination) =>
        localNode.get() match {
          case Some(localhost) =>
            getDestinationLocation(servletRequest, destination, localhost, routeDest.getUri, servletRequest) match {
              case Some(locationUrl) =>
                // According to the Java 6 javadocs the routeDestination passed into getContext:
                // "The given path [routeDestination] must begin with /, is interpreted relative to the server's document root
                // and is matched against the context roots of other web applications hosted on this container."
                Option(getServletContext.getContext(locationUrl.getPath)).foreach { targetContext =>
                  // Capture this for Location header processing
                  val locationUrlPath = locationUrl.getPath
                  val targetContextPath = targetContext.getContextPath
                  val dispatchPath = if (locationUrlPath.startsWith(targetContextPath)) locationUrlPath.substring(targetContextPath.length) else locationUrlPath
                  Option(targetContext.getRequestDispatcher(dispatchPath)) match {
                    case Some(dispatcher) =>
                      doDispatch(
                        servletRequest,
                        servletResponse,
                        locationUrl,
                        dispatchPath,
                        targetContext,
                        routeDest,
                        dispatcher,
                        destination)
                    case _ =>
                      logger.warn(s"No Request Dispatcher found in Servlet Context for $dispatchPath; not forwarding")
                  }
                }
              case _ =>
                logger.warn("No route-able node found; not forwarding")
            }
          case _ =>
            logger.warn("No valid local host defined; not forwarding")
        }
      case id =>
        val localId = localCluster.get().map(_.getId) match {
          case Some(locId) => locId
          case None => ""
        }
        logger.warn("Invalid routing destination specified: {} for cluster: {} ", id, localId)
        servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }

  def getDestinationLocation(servletRequest: HttpServletRequestWrapper, destination: Destination, localhost: Node, uri: String, request: HttpServletRequest): Option[URL] = {
    val http = if (localhost.getHttpPort > 0) List(new Port("http", localhost.getHttpPort)) else List.empty[Port]
    val https = if (localhost.getHttpsPort > 0) List(new Port("https", localhost.getHttpsPort)) else List.empty[Port]
    val localPorts = http ++ https

    destination match {
      case endpoint: DestinationEndpoint =>
        Some(buildEndpoint(localhost, localPorts, endpoint, uri, servletRequest))
      case cluster: DestinationCluster =>
        // TODO: Remove this when routing to a cluster is removed.
        domains.get().map(_.getDomain(cluster.getCluster.getId)) match {
          case Some(clusterWrapper) =>
            val routableNode = clusterWrapper.getNextNode
            val port = if (HttpsProtocol.equalsIgnoreCase(cluster.getProtocol)) routableNode.getHttpsPort else routableNode.getHttpPort
            Some(new URL(cluster.getProtocol, routableNode.getHostname, port, cluster.getRootPath + uri))
          case None =>
            logger.warn("No routable node for domain: " + cluster.getId)
            None
        }
      case _ =>
        logger.warn(s"Unknown destination type: ${destination.getClass.getName}")
        None
    }
  }

  def buildEndpoint(localhost: Node, localPorts: List[Port], endpoint: DestinationEndpoint, uri: String, request: HttpServletRequest): URL = {
    val protocol = endpoint.getProtocol
    val endpointPort = endpoint.getPort
    val endpointHost = endpoint.getHostname
    val path = StringUriUtilities.concatUris(endpoint.getRootPath, uri)

    val portUrl = if (!StringUtils.isBlank(protocol)) {
      val portNum = if (endpointPort <= 0) {
        localPorts.find(_.getProtocol.equalsIgnoreCase(protocol)) match {
          case Some(port) => port.getNumber
          case _ => 0
        }
      } else {
        endpointPort
      }
      new Port(protocol, portNum)
    } else {
      val port = new Port(request.getScheme, request.getLocalPort)
      if (localPorts.contains(port)) {
        port
      } else {
        throw new MalformedURLException("Cannot determine destination port.")
      }
    }
    // IF the endpoint hostname is blank, THEN it is local
    val hostnameUrl = if (StringUtils.isBlank(endpointHost)) localhost.getHostname else endpointHost

    new URL(portUrl.getProtocol, hostnameUrl, portUrl.getNumber, path)
  }

  def doDispatch(servletRequest: HttpServletRequestWrapper,
                 servletResponse: HttpServletResponse,
                 locationUrl: URL,
                 uri: String,
                 targetContext: ServletContext,
                 routeDest: RouteDestination,
                 dispatcher: RequestDispatcher,
                 destination: Destination): Unit = {
    servletRequest.setScheme(locationUrl.getProtocol)
    servletRequest.setServerName(locationUrl.getHost)
    servletRequest.setServerPort(locationUrl.getPort)
    servletRequest.setRequestURI(locationUrl.getPath)

    requestHeaderService.setVia(servletRequest)
    requestHeaderService.setXForwardedFor(servletRequest)
    logger.debug("Attempting to route to: {}", locationUrl.getPath)
    logger.debug("  Using dispatcher for: {}", uri)
    logger.debug("           Request URL: {}", servletRequest.getRequestURL)
    logger.debug("           Request URI: {}", servletRequest.getRequestURI)
    logger.debug("          Context path: {}", targetContext.getContextPath)
    val startTime = System.currentTimeMillis
    try {
      reportingService.incrementRequestCount(routeDest.getDestinationId)
      dispatcher.forward(servletRequest, servletResponse)
      // track response code for endpoint & across all endpoints
      val endpoint = getEndpoint(destination, locationUrl)
      val servletResponseStatus = servletResponse.getStatus
      metricsService.foreach { metSer =>
        markResponseCodeHelper(metSer, servletResponseStatus, endpoint)
        markResponseCodeHelper(metSer, servletResponseStatus, AllEndpoints)
        markRequestTimeoutHelper(metSer, servletResponseStatus, endpoint)
        markRequestTimeoutHelper(metSer, servletResponseStatus, AllEndpoints)
      }
      val stopTime = System.currentTimeMillis
      reportingService.recordServiceResponse(routeDest.getDestinationId, servletResponseStatus, stopTime - startTime)
      responseHeaderService.fixLocationHeader(
        servletRequest.getRequest.asInstanceOf[HttpServletRequest],
        servletResponse,
        routeDest,
        locationUrl.getPath,
        destination.getRootPath)
    } catch {
      case e: IOException =>
        if (e.getCause.isInstanceOf[ReadLimitReachedException]) {
          logger.error("Error reading request content", e)
          servletResponse.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Error reading request content")
        }
        else {
          logger.error("Error communicating with {}", locationUrl.getPath, e)
          servletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
        }
    }
  }

  def getEndpoint(dest: Destination, locationUrl: URL): String = {
    val sb = new StringBuilder
    sb.append(locationUrl.getHost).append(":").append(locationUrl.getPort)
    dest match {
      case endpoint: DestinationEndpoint => sb.append(endpoint.getRootPath)
      case cluster: DestinationCluster => sb.append(cluster.getRootPath)
      case _ => throw new IllegalArgumentException("Unknown destination type: " + dest.getClass.getName)
    }
    sb.toString
  }

  def markResponseCodeHelper(metricsService: MetricsService, responseCode: Int, component: String): Unit = {
    if (100 < responseCode && responseCode < 600) {
      metricsService
        .getRegistry
        .meter(MetricRegistry.name("org.openrepose.core.ResponseCode", component, "%dXX".format(responseCode / 100)))
        .mark()
    } else {
      logger.error(s"$component Encountered invalid response code: $responseCode")
    }
  }

  def markRequestTimeoutHelper(metricsService: MetricsService, responseCode: Int, endpoint: String): Unit = {
    if (responseCode == SC_REQUEST_TIMEOUT) {
      metricsService
        .getRegistry
        .meter(MetricRegistry.name("org.openrepose.core.RequestTimeout.TimeoutToOrigin", endpoint))
        .mark()
    }
  }
}

object ReposeRoutingServlet {
  final val SystemModelConfigurationFilename = "system-model.cfg.xml"
  final val NotInitializedMessage = "The ReposeRoutingServlet has not yet been initialized"
  final val FailedToRouteMessage = "Failed to route the request to the origin service"
  private val AllEndpoints = "All Endpoints"
  private val HttpsProtocol = "https"
}
