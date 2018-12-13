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
import java.net.{URI, URL}
import java.util.Optional

import com.codahale.metrics.MetricRegistry
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import org.apache.http.HttpVersion
import org.apache.http.client.methods.{CloseableHttpResponse, HttpUriRequest}
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, contains, eq => isEq}
import org.mockito.Mockito._
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.commons.utils.servlet.http.RouteDestination
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.systemmodel.config._
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.openrepose.powerfilter.ReposeRoutingServletTest._
import org.scalatest.TryValues._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse, MockServletConfig}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ReposeRoutingServletTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {
  var mockServletConfig: ServletConfig = _
  var mockConfigurationService: ConfigurationService = _
  var mockContainerConfigurationService: ContainerConfigurationService = _
  var mockHttpClient: HttpClientServiceClient = _
  var mockHttpClientService: HttpClientService = _
  var metricRegistry: MetricRegistry = _
  var mockMetricsService: MetricsService = _
  var reposeRoutingServlet: ReposeRoutingServlet = _
  var listAppender: ListAppender = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    mockServletConfig = new MockServletConfig()
    mockConfigurationService = mock[ConfigurationService]
    mockContainerConfigurationService = mock[ContainerConfigurationService]
    mockHttpClient = mock[HttpClientServiceClient]
    mockHttpClientService = mock[HttpClientService]
    metricRegistry = spy(new MetricRegistry)
    mockMetricsService = mock[MetricsService]

    when(mockMetricsService.getRegistry).thenReturn(metricRegistry)
    when(mockContainerConfigurationService.getRequestVia).thenReturn(Optional.empty[String]())
    when(mockHttpClientService.getDefaultClient).thenReturn(mockHttpClient)
    when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext])).thenReturn(
      new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_OK, null) with CloseableHttpResponse {
        override def close(): Unit = {}
      }
    )

    reposeRoutingServlet = new ReposeRoutingServlet(
      DefaultVersion,
      DefaultClusterId,
      DefaultNodeId,
      mockConfigurationService,
      mockContainerConfigurationService,
      mockHttpClientService,
      Optional.of(mockMetricsService))

    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
  }

  describe("init") {
    it("should register configuration listener") {
      reposeRoutingServlet.init(mockServletConfig)

      verify(mockConfigurationService).subscribeTo(
        isEq(ReposeRoutingServlet.SystemModelConfigurationFilename),
        any[URL](),
        any[UpdateListener[SystemModel]](),
        any[Class[SystemModel]]()
      )
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Reticulating Splines - Initializing Repose Routing Servlet")) shouldBe 1
    }
  }

  describe("destroy") {
    it("should unregister configuration listener") {
      reposeRoutingServlet.destroy()

      verify(mockConfigurationService).unsubscribeFrom(
        isEq(ReposeRoutingServlet.SystemModelConfigurationFilename),
        any[UpdateListener[SystemModel]]()
      )
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Obfuscated Quigley Matrix - Destroyed Repose Routing Servlet")) shouldBe 1
    }
  }

  describe("isInitialized") {
    it("should return false if a configuration has not yet been read") {
      reposeRoutingServlet.isInitialized shouldBe false
    }

    it("should return true if a configuration has been read") {
      reposeRoutingServlet.configurationUpdated(minimalConfiguration())

      reposeRoutingServlet.isInitialized shouldBe true
    }
  }

  describe("service") {
    Seq(
      // @formatter:off
      ("one", SC_OK,                       null),
      ("one", SC_BAD_GATEWAY,              new IOException()),
      ("one", SC_REQUEST_ENTITY_TOO_LARGE, new IOException().initCause(new ReadLimitReachedException("too much"))),
      ("two", SC_OK,                       null),
      ("two", SC_BAD_GATEWAY,              new IOException()),
      ("two", SC_REQUEST_ENTITY_TOO_LARGE, new IOException().initCause(new ReadLimitReachedException("too much")))
      // @formatter:on
    ).foreach { case (defaultDestinationId, responseCode, dispatchError) =>
      Seq(null, 0, 8080, 8088).foreach { port =>
        it(s"port number: $port, route default destination: $defaultDestinationId, response code: $responseCode, dispatch error: ${Option(dispatchError).flatMap(e => Option(e.getCause).map(ie => ie.getMessage).orElse(Option("None"))).getOrElse("Null")}") {
          val request = new MockHttpServletRequest("POST", "/this/is/the/requestURI")
          request.setScheme("http")
          request.setServerName(DefaultNodeName)
          request.setServerPort(8080)
          val response = new MockHttpServletResponse()
          val systemModel = minimalConfiguration()
          val destinationEndpointList = systemModel.getReposeCluster.headOption.map(_.getDestinations).map(_.getEndpoint)
          destinationEndpointList.foreach(_.clear())
          val destinationEndpointOne = new DestinationEndpoint()
          destinationEndpointOne.setDefault("one".equals(defaultDestinationId))
          destinationEndpointOne.setId("one")
          destinationEndpointOne.setProtocol("http")
          Option(port).foreach(p => destinationEndpointOne.setPort(p.asInstanceOf[Int]))
          destinationEndpointList.foreach(_.add(destinationEndpointOne))
          val destinationEndpointTwo = new DestinationEndpoint()
          destinationEndpointTwo.setDefault("two".equals(defaultDestinationId))
          destinationEndpointTwo.setId("two")
          destinationEndpointTwo.setProtocol("http")
          destinationEndpointList.foreach(_.add(destinationEndpointTwo))
          val servletContext = mock[ServletContext]

          mockServletConfig = new MockServletConfig(servletContext)

          Option(dispatchError).foreach { error =>
            when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext])).thenThrow(error)
          }

          reposeRoutingServlet.init(mockServletConfig)
          reposeRoutingServlet.configurationUpdated(systemModel)
          reposeRoutingServlet.service(request, response)

          response.getStatus == responseCode

          verify(mockHttpClient).execute(any[HttpUriRequest], any[HttpContext])
          if (Option(dispatchError).isEmpty) {
            verify(metricRegistry, times(2)).meter(contains("ResponseCode"))
            verify(metricRegistry, times(2)).timer(contains("ResponseTime"))
          }
        }
      }
    }

    it(s"should return the existing status code if the response is already committed") {
      val req = new MockHttpServletRequest()
      val resp = new MockHttpServletResponse()

      resp.sendError(SC_NOT_ACCEPTABLE)

      reposeRoutingServlet.init(mockServletConfig)
      reposeRoutingServlet.configurationUpdated(minimalConfiguration())
      reposeRoutingServlet.service(req, resp)

      resp.getStatus shouldBe SC_NOT_ACCEPTABLE
    }
  }

  describe("getRoute") {
    it("should return a Failure if the destinations attribute object is not of a supported type") {
      val request = new MockHttpServletRequest()
      val systemModel = minimalConfiguration()
      request.setAttribute(CommonRequestAttributes.DESTINATIONS, Set(
        new RouteDestination("destinationOne", "", 1.0),
        new RouteDestination("destinationTwo", "", 0.5)
      ))

      reposeRoutingServlet.configurationUpdated(systemModel)

      val route = reposeRoutingServlet.getRoute(request)

      route.isFailure shouldBe true
    }

    it("should return a route to the default destination if no other routes are available") {
      val request = new MockHttpServletRequest()
      val systemModel = minimalConfiguration()
      val destinationOne = { val destination = new DestinationEndpoint(); destination.setId("destinationOne"); destination }
      val destinationTwo = { val destination = new DestinationEndpoint(); destination.setId("destinationTwo"); destination }
      systemModel.getReposeCluster.head.getDestinations.getEndpoint.addAll(Seq(
        destinationOne,
        destinationTwo
      ))

      reposeRoutingServlet.configurationUpdated(systemModel)

      val route = reposeRoutingServlet.getRoute(request)

      route.success.value.getDestinationId shouldEqual DefaultDestId
    }

    it("should return the highest quality route available (Java)") {
      import scala.collection.JavaConverters._

      val request = new MockHttpServletRequest()
      val systemModel = minimalConfiguration()
      val destinationOne = { val destination = new DestinationEndpoint(); destination.setId("destinationOne"); destination }
      val destinationTwo = { val destination = new DestinationEndpoint(); destination.setId("destinationTwo"); destination }
      systemModel.getReposeCluster.head.getDestinations.getEndpoint.addAll(Seq(
        destinationOne,
        destinationTwo
      ))
      request.setAttribute(CommonRequestAttributes.DESTINATIONS, List(
        new RouteDestination(destinationOne.getId, "", 1.0),
        new RouteDestination(destinationTwo.getId, "", 0.5)
      ).asJava)

      reposeRoutingServlet.configurationUpdated(systemModel)

      val route = reposeRoutingServlet.getRoute(request)

      route.success.value.getDestinationId shouldEqual destinationOne.getId
    }

    it("should return the highest quality route available (Scala)") {
      val request = new MockHttpServletRequest()
      val systemModel = minimalConfiguration()
      val destinationOne = { val destination = new DestinationEndpoint(); destination.setId("destinationOne"); destination }
      val destinationTwo = { val destination = new DestinationEndpoint(); destination.setId("destinationTwo"); destination }
      systemModel.getReposeCluster.head.getDestinations.getEndpoint.addAll(Seq(
        destinationOne,
        destinationTwo
      ))
      request.setAttribute(CommonRequestAttributes.DESTINATIONS, Seq(
        new RouteDestination(destinationOne.getId, "", 1.0),
        new RouteDestination(destinationTwo.getId, "", 0.5)
      ))

      reposeRoutingServlet.configurationUpdated(systemModel)

      val route = reposeRoutingServlet.getRoute(request)

      route.success.value.getDestinationId shouldEqual destinationOne.getId
    }
  }

  describe("getDestination") {
    it("should return a Failure if no potential destination corresponds to the route") {
      val destinationOne = { val destination = new DestinationEndpoint(); destination.setId("destinationOne"); destination }
      val destinationTwo = { val destination = new DestinationEndpoint(); destination.setId("destinationTwo"); destination }
      val route = new RouteDestination("not-a-destination", "/", 0.0)
      val systemModel = minimalConfiguration()
      systemModel.getReposeCluster.head.getDestinations.getEndpoint.addAll(Seq(
        destinationOne,
        destinationTwo
      ))

      reposeRoutingServlet.configurationUpdated(systemModel)

      val destination = reposeRoutingServlet.getDestination(route)

      destination.isFailure shouldBe true
    }

    it("should return the destination corresponding to the route") {
      val destinationOne = { val destination = new DestinationEndpoint(); destination.setId("destinationOne"); destination }
      val destinationTwo = { val destination = new DestinationEndpoint(); destination.setId("destinationTwo"); destination }
      val route = new RouteDestination(destinationOne.getId, "/", 0.0)
      val systemModel = minimalConfiguration()
      systemModel.getReposeCluster.head.getDestinations.getEndpoint.addAll(Seq(
        destinationOne,
        destinationTwo
      ))

      reposeRoutingServlet.configurationUpdated(systemModel)

      val destination = reposeRoutingServlet.getDestination(route)

      destination.success.value shouldBe destinationOne
    }
  }

  describe("getTarget") {
    it("should return a Failure when the destination is invalid") {
      val route = new RouteDestination("id", "/some/resource", 0.0)
      val destination = new DestinationEndpoint()
      destination.setChunkedEncoding(ChunkedEncoding.AUTO)
      destination.setProtocol("ht tp")
      destination.setHostname("example.com")
      destination.setPort(8080)
      destination.setRootPath("/root")

      val tryTarget = reposeRoutingServlet.getTarget(route, destination)

      tryTarget.isFailure shouldBe true
    }

    it("should encode an invalid route and return a URL locating the correct resource") {
      val route = new RouteDestination("id", "/some/re source", 0.0)
      val destination = new DestinationEndpoint()
      destination.setChunkedEncoding(ChunkedEncoding.AUTO)
      destination.setProtocol("http")
      destination.setHostname("example.com")
      destination.setPort(8080)
      destination.setRootPath("/root")

      val tryTarget = reposeRoutingServlet.getTarget(route, destination)

      val target = tryTarget.success.value
      target.chunkedEncoding shouldBe destination.getChunkedEncoding
      target.url.getProtocol shouldEqual destination.getProtocol
      target.url.getHost shouldEqual destination.getHostname
      target.url.getPort shouldEqual destination.getPort
      target.url.getPath shouldEqual (destination.getRootPath + route.getUri.replace(" ", "%20"))
    }

    it("should return a URL locating the correct resource") {
      val route = new RouteDestination("id", "/some/resource", 0.0)
      val destination = new DestinationEndpoint()
      destination.setChunkedEncoding(ChunkedEncoding.AUTO)
      destination.setProtocol("http")
      destination.setHostname("example.com")
      destination.setPort(8080)
      destination.setRootPath("/root")

      val tryTarget = reposeRoutingServlet.getTarget(route, destination)

      val target = tryTarget.success.value
      target.chunkedEncoding shouldBe destination.getChunkedEncoding
      target.url.getProtocol shouldEqual destination.getProtocol
      target.url.getHost shouldEqual destination.getHostname
      target.url.getPort shouldEqual destination.getPort
      target.url.getPath shouldEqual (destination.getRootPath + route.getUri)
    }

    it("should return a URL locating the correct resource when the destination does not have a set port") {
      val route = new RouteDestination("id", "/some/resource", 0.0)
      val destination = new DestinationEndpoint()
      destination.setChunkedEncoding(ChunkedEncoding.AUTO)
      destination.setProtocol("http")
      destination.setHostname("example.com")
      destination.setRootPath("/root")

      val tryTarget = reposeRoutingServlet.getTarget(route, destination)

      val target = tryTarget.success.value
      target.chunkedEncoding shouldBe destination.getChunkedEncoding
      target.url.getProtocol shouldEqual destination.getProtocol
      target.url.getHost shouldEqual destination.getHostname
      target.url.getPort shouldEqual -1
      target.url.getPath shouldEqual (destination.getRootPath + route.getUri)
    }
  }

  describe("preProxyMetrics") {
    it("should increment the request count") {
      val destId = "testId"
      val destination = new DestinationEndpoint()
      destination.setId(destId)

      reposeRoutingServlet.preProxyMetrics(destination)

      verify(metricRegistry).meter(contains(s"RequestDestination.$destId"))
    }
  }

  describe("postProxyMetrics") {
    it("should record the service response") {
      val timeElapsed = 1234L
      val response = new MockHttpServletResponse()
      val destination = new DestinationEndpoint()
      val targetUrl = URI.create("http://example.com/some/path").toURL
      response.setStatus(201)
      destination.setId("testId")

      reposeRoutingServlet.postProxyMetrics(timeElapsed, response, destination, targetUrl)

      verify(metricRegistry, atLeastOnce()).meter(contains(".Response"))
    }
  }

  def minimalConfiguration(): SystemModel = {
    val systemModel = new SystemModel()
    val reposeCluster = new ReposeCluster()
    reposeCluster.setId(DefaultClusterId)
    val node = new Node()
    node.setId(DefaultNodeId)
    node.setHostname(DefaultNodeName)
    node.setHttpPort(8080)
    node.setHttpsPort(8443)
    val nodesList = new NodeList()
    nodesList.getNode.add(node)
    reposeCluster.setNodes(nodesList)
    val destinationEndpoint = new DestinationEndpoint()
    destinationEndpoint.setDefault(true)
    destinationEndpoint.setId(DefaultDestId)
    val destinationList = new DestinationList()
    destinationList.getEndpoint.add(destinationEndpoint)
    reposeCluster.setDestinations(destinationList)
    systemModel.getReposeCluster.add(reposeCluster)
    systemModel
  }
}

object ReposeRoutingServletTest {
  final val DefaultVersion = "0.0.0.0"
  final val DefaultClusterId = "defaultClusterId"
  final val DefaultNodeId = "defaultNodeId"
  final val DefaultNodeName = "defaultNodeName"
  final val DefaultDestId = "defaultDestId"
}
