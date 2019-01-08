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

import java.util.{Optional, UUID}

import com.codahale.metrics.MetricRegistry
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Scope, Span, SpanContext, Tracer}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.methods.HttpGet
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyBoolean, anyString, contains}
import org.mockito.Mockito._
import org.openrepose.commons.test.MockitoAnswers
import org.openrepose.commons.utils.http.CommonHttpHeader.TRACE_GUID
import org.openrepose.commons.utils.http.PowerApiHeader.TRACE_REQUEST
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.commons.utils.logging.TracingKey.TRACING_KEY
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.openrepose.core.systemmodel.config.{TracingHeaderConfig, Filter => FilterConfig}
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.openrepose.powerfilter.ReposeFilterLoader.{FilterContext, FilterContextList, FilterContextRegistrar}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.slf4j.MDC
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.mock.web.{MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ReposeFilterTest extends FunSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach
  with MockitoAnswers {

  var nodeId: String = _
  var reposeVersion: String = _
  var metricRegistry: MetricRegistry = _
  var mockMetricsService: MetricsService = _
  var optMetricsService: Optional[MetricsService] = _
  var reposeFilterLoader: ReposeFilterLoader = _
  var containerConfigurationService: ContainerConfigurationService = _
  var healthCheckService: HealthCheckService = _
  var tracer: Tracer = _
  var spanBuilder: SpanBuilder = _
  var scope: Scope = _
  var span: Span = _
  var uriRedactionService: UriRedactionService = _
  var responseHeaderService: ResponseHeaderService = _
  var filter: ReposeFilter = _
  var mockFilter: Filter = _
  var mockAbstractApplicationContext: AbstractApplicationContext = _
  var filterConfig: FilterConfig = _
  var filterContexts: List[FilterContext] = _
  var filterContextList: FilterContextList = _
  var request: MockHttpServletRequest = _
  var response: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  var mdcValue: String = _
  var loggerContext: LoggerContext = _
  var listAppender: ListAppender = _

  override def beforeEach(): Unit = {
    nodeId = "randomNode"
    reposeVersion = "0.1.2.3"
    metricRegistry = spy(new MetricRegistry)
    mockMetricsService = mock[MetricsService]
    when(mockMetricsService.getRegistry).thenReturn(metricRegistry)
    optMetricsService = Optional.ofNullable(mockMetricsService)
    reposeFilterLoader = mock[ReposeFilterLoader]
    when(reposeFilterLoader.getTracingHeaderConfig).thenReturn(None)
    containerConfigurationService = mock[ContainerConfigurationService]
    when(containerConfigurationService.getContentBodyReadLimit).thenReturn(Optional.ofNullable[java.lang.Long](null))
    healthCheckService = mock[HealthCheckService]
    when(healthCheckService.isHealthy).thenReturn(true)
    tracer = mock[Tracer]
    spanBuilder = mock[SpanBuilder]
    scope = mock[Scope]
    span = mock[Span]
    when(scope.span()).thenReturn(span)
    when(spanBuilder.startActive(anyBoolean())).thenReturn(scope)
    when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
    when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
    when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
    uriRedactionService = mock[UriRedactionService]
    responseHeaderService = mock[ResponseHeaderService]
    mockFilter = mock[Filter]
    mockAbstractApplicationContext = mock[AbstractApplicationContext]
    filterConfig = new FilterConfig
    filterConfig.setName("foo")
    filterContexts = List(FilterContext(mockFilter, filterConfig, (request: HttpServletRequest) => true, mockAbstractApplicationContext))
    filterContextList = new FilterContextList(mock[FilterContextRegistrar], filterContexts, None)
    when(reposeFilterLoader.getFilterContextList).thenReturn(Option(filterContextList))
    filter = new ReposeFilter(
      nodeId,
      reposeVersion,
      optMetricsService,
      reposeFilterLoader,
      containerConfigurationService,
      healthCheckService,
      tracer,
      uriRedactionService,
      responseHeaderService
    )
    request = new MockHttpServletRequest(HttpGet.METHOD_NAME, "/")
    response = new MockHttpServletResponse
    filterChain = mock[FilterChain]
    loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
    listAppender.clear()
  }

  describe("init method") {
    it("should result in the servlet context being set in the Repose Filter Loader.") {
      filter.init(new MockFilterConfig)

      verify(reposeFilterLoader).setServletContext(any(classOf[ServletContext]))
    }
  }

  describe("destroy method") {
    it("should not throw any exceptions") {
      filter.destroy()
    }
  }

  describe("doFilter method") {
    it("should return an Internal Server Error (500) if filter loader is not initialized") {
      when(reposeFilterLoader.getFilterContextList).thenReturn(None)

      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
      response.getErrorMessage shouldBe "ReposeFilter not initialized"
    }

    it("should return an Internal Server Error (503) if not healthy") {
      when(healthCheckService.isHealthy).thenReturn(false)

      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      response.getErrorMessage shouldBe "Currently unable to serve requests"
    }

    it("should call through to the filter in the FilterContextList") {
      filter.doFilter(request, response, filterChain)

      verify(mockFilter).doFilter(any(classOf[HttpServletRequest]), any(classOf[HttpServletResponse]), any(classOf[FilterChain]))
    }

    it("should start a span") {
      filter.doFilter(request, response, filterChain)

      verify(spanBuilder, atLeastOnce).startActive(true)
    }

    it("should start a span even with an empty filter list") {
      filterContextList = new FilterContextList(mock[FilterContextRegistrar], List.empty[FilterContext], None)

      filter.doFilter(request, response, filterChain)

      verify(spanBuilder, atLeastOnce).startActive(true)
    }

    it("should close the span") {
      filter.doFilter(request, response, filterChain)

      verify(scope, atLeastOnce).close()
    }

    it("should close the span even with an empty filter list") {
      filterContextList = new FilterContextList(mock[FilterContextRegistrar], List.empty[FilterContext], None)

      filter.doFilter(request, response, filterChain)

      verify(scope, atLeastOnce).close()
    }

    it("should log and return Ok (200) on a success") {
      filter.doFilter(request, response, filterChain)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Successfully processed request")) should have length 1
      response.getStatus shouldBe SC_OK
    }

    Set("CONNECT", "NONSTANDARD").foreach { method =>
      it(s"should return a 400 if the request method is not supported: $method") {
        request.setMethod(method)

        filter.doFilter(request, response, filterChain)

        val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
        messageList.filter(_.contains("Invalid HTTP method requested:")) should have length 1
        response.getStatus shouldBe SC_BAD_REQUEST
      }
    }

    it("should log and return Bad Request (400) on a malformed URI") {
      request.setRequestURI("\\BAD_URI\\")

      filter.doFilter(request, response, filterChain)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Invalid URI requested:")) should have length 1
      response.getStatus shouldBe SC_BAD_REQUEST
    }

    it("should log and return Bad Gateway (502) when an exception is caught") {
      when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenThrow(new RuntimeException("abc"))

      val spyResponse = spy(response)
      when(spyResponse.isCommitted).thenThrow(new RuntimeException("def")).thenReturn(false)

      filter.doFilter(request, spyResponse, filterChain)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Issue encountered while processing filter chain.")) should have length 1
      spyResponse.getStatus shouldBe SC_BAD_GATEWAY
    }

    it("should return Bad Gateway (502) and rethrow the throwable") {
      when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenAnswer(answer({ _ =>
        throw new Throwable("abc")
      }))

      intercept[Throwable] {
        filter.doFilter(request, response, filterChain)
      }

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Error encountered while processing filter chain.")) should have length 1
      response.getStatus shouldBe SC_BAD_GATEWAY
    }

    it("should verify TRACE_REQUEST was not put in the MDC") {
      when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenAnswer(answer({ _ =>
        mdcValue = MDC.get(TRACE_REQUEST)
      }))

      filter.doFilter(request, response, filterChain)

      assertTrue("MDC Value should be blank.", StringUtils.isBlank(mdcValue))
    }

    it("should verify TRACE_REQUEST was put in the MDC") {
      when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenAnswer(answer({ _ =>
        mdcValue = MDC.get(TRACE_REQUEST)
      }))
      request.addHeader(TRACE_REQUEST, true)

      filter.doFilter(request, response, filterChain)

      mdcValue shouldBe "true"
    }

    it("should verify random TRACING_KEY was put in the MDC") {
      when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenAnswer(answer({ _ =>
        mdcValue = MDC.get(TRACING_KEY)
      }))

      filter.doFilter(request, response, filterChain)

      assertTrue("MDC Value should NOT be blank.", StringUtils.isNotBlank(mdcValue))
    }

    it("should verify existing TRACING_KEY was put in the MDC") {
      when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenAnswer(answer({ _ =>
        mdcValue = MDC.get(TRACING_KEY)
      }))
      val traceGUID = UUID.randomUUID.toString
      val traceHeader = TracingHeaderHelper.createTracingHeader(traceGUID, "origin")
      request.addHeader(TRACE_GUID, traceHeader)

      filter.doFilter(request, response, filterChain)

      mdcValue shouldBe traceGUID
    }

    it("should verify all values put in the MDC are cleared") {
      val traceGUID = UUID.randomUUID.toString
      val traceHeader = TracingHeaderHelper.createTracingHeader(traceGUID, "origin")
      request.addHeader(TRACE_REQUEST, true)
      request.addHeader(TRACE_GUID, traceHeader)

      filter.doFilter(request, response, filterChain)

      assertTrue("MDC Values should be blank.", StringUtils.isBlank(MDC.get(TRACE_REQUEST)))
      assertTrue("MDC Values should be blank.", StringUtils.isBlank(MDC.get(TRACING_KEY)))
    }

    it("should set a tracing attribute if a tracing header should be set on the response") {
      filter.doFilter(request, response, filterChain)

      request.getAttribute(TRACE_GUID) should not be null
    }

    it("should not set a tracing attribute if a tracing header should not be set on the response") {
      when(reposeFilterLoader.getTracingHeaderConfig).thenReturn(Some {
        val config = new TracingHeaderConfig
        config.setEnabled(false)
        config
      })

      filter.doFilter(request, response, filterChain)

      request.getAttribute(TRACE_GUID) shouldBe null
    }
  }

  it("should update metrics with OK (200)") {
    filter.doFilter(request, response, filterChain)

    verify(metricRegistry).meter(contains("2XX"))
    verify(metricRegistry).timer(contains("2XX"))
  }

  it("should update metrics with Bad Request (400)") {
    request.setRequestURI("\\BAD_URI\\")

    filter.doFilter(request, response, filterChain)

    verify(metricRegistry).meter(contains("4XX"))
    verify(metricRegistry).timer(contains("4XX"))
  }

  it("should log when status code is invalid and not update the metrics") {
    when(mockFilter.doFilter(any[HttpServletRequest], any[HttpServletResponse], any[FilterChain])).thenAnswer(answer({ invocation =>
      invocation.getArguments()(1).asInstanceOf[HttpServletResponse].sendError(600)
    }))

    filter.doFilter(request, response, filterChain)

    val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
    messageList.filter(_.contains("Encountered invalid response code:")) should have length 1
    response.getStatus shouldBe 600
    verify(metricRegistry, never).meter(contains("org.openrepose.core.ResponseCode.Repose"))
    verify(metricRegistry, never).timer(contains("org.openrepose.core.ResponseTime.Repose"))
  }
}
