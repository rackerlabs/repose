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
import java.net.URL

import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, eq => isEq}
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.core.ReposeRoutingServletTest._
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.ReportingService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.systemmodel.config._
import org.openrepose.nodeservice.request.RequestHeaderService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse, MockServletConfig}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ReposeRoutingServletTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {
  var mockServletConfig: ServletConfig = _
  var mockConfigurationService: ConfigurationService = _
  var mockRequestHeaderService: RequestHeaderService = _
  var mockResponseHeaderService: ResponseHeaderService = _
  var mockReportingService: ReportingService = _
  var mockMetricsService: Option[MetricsService] = _
  var reposeRoutingServlet: ReposeRoutingServlet = _
  var listAppender: ListAppender = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    mockServletConfig = new MockServletConfig()
    mockConfigurationService = mock[ConfigurationService]
    mockRequestHeaderService = mock[RequestHeaderService]
    mockResponseHeaderService = mock[ResponseHeaderService]
    mockReportingService = mock[ReportingService]
    mockMetricsService = None
    reposeRoutingServlet = new ReposeRoutingServlet(
      DefaultClusterId,
      DefaultNodeId,
      mockConfigurationService,
      mockRequestHeaderService,
      mockResponseHeaderService,
      mockReportingService,
      mockMetricsService)

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
      events.count(_.contains("Obfuscated Quigley Matrix - Destroying Repose Routing Servlet")) shouldBe 1
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
      (null,  SC_OK,                       null),
      (null,  SC_OK,                       new IOException()),
      (null,  SC_OK,                       new IOException().initCause(new ReadLimitReachedException("too much"))),
      ("one", SC_OK,                       null),
      ("one", SC_SERVICE_UNAVAILABLE,      new IOException()),
      ("one", SC_REQUEST_ENTITY_TOO_LARGE, new IOException().initCause(new ReadLimitReachedException("too much"))),
      ("two", SC_OK,                       null),
      ("two", SC_SERVICE_UNAVAILABLE,      new IOException()),
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
          val targetServletContext = mock[ServletContext]
          val requestDispatcher = mock[RequestDispatcher]

          when(servletContext.getContext(anyString())).thenReturn(targetServletContext)
          when(targetServletContext.getRequestDispatcher(anyString())).thenReturn(requestDispatcher)
          when(targetServletContext.getContextPath).thenReturn("")
          mockServletConfig = new MockServletConfig(servletContext)

          if (dispatchError != null) {
            when(requestDispatcher.forward(any(classOf[ServletRequest]), any(classOf[ServletResponse]))).thenThrow(dispatchError)
          }

          reposeRoutingServlet.init(mockServletConfig)
          reposeRoutingServlet.configurationUpdated(systemModel)
          reposeRoutingServlet.service(request, response)

          response.getStatus == responseCode

          if (defaultDestinationId == null) {
            verify(requestDispatcher, never()).forward(any(classOf[ServletRequest]), any(classOf[ServletResponse]))
            verify(mockReportingService, never()).incrementRequestCount(anyString())
          } else {
            verify(requestDispatcher).forward(any(classOf[ServletRequest]), any(classOf[ServletResponse]))
            verify(mockReportingService).incrementRequestCount(anyString())
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
  final val DefaultClusterId = "defaultClusterId"
  final val DefaultNodeId = "defaultNodeId"
  final val DefaultNodeName = "defaultNodeName"
  final val DefaultDestId = "defaultDestId"
}
