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

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{MetricRegistry, Timer}
import io.opentracing.mock.MockTracer
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{Filter, FilterChain}
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.test.appender.ListAppender
import org.apache.logging.log4j.{Level, LogManager}
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, anyLong, same, eq => meq}
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.commons.utils.http.PowerApiHeader.TRACE_REQUEST
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.httplogging.HttpLoggingContext
import org.openrepose.core.systemmodel.config.{Filter => FilterConfig}
import org.openrepose.powerfilter.ReposeFilterLoader.FilterContext
import org.scalatest.LoneElement._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.slf4j.MDC
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ReposeFilterChainTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  var mockFilter: Filter = _
  var fooFilterConfig: FilterConfig = _
  var barFilterConfig: FilterConfig = _
  var originalChain: FilterChain = _
  var timer: Timer = _
  var metricsRegistry: MetricRegistry = _
  var loggingContext: HttpLoggingContext = _
  var tracer: MockTracer = _
  var mockRequest: MockHttpServletRequest = _
  var mockResponse: MockHttpServletResponse = _

  override protected def beforeEach(): Unit = {
    mockFilter = mock[Filter]
    fooFilterConfig = new FilterConfig
    fooFilterConfig.setName("foo")
    barFilterConfig = new FilterConfig
    barFilterConfig.setName("bar")
    originalChain = mock[FilterChain]
    timer = mock[Timer]
    metricsRegistry = mock[MetricRegistry]
    when(metricsRegistry.timer(any[String])).thenReturn(timer)
    loggingContext = mock[HttpLoggingContext]
    tracer = new MockTracer()
    mockRequest = new MockHttpServletRequest("GET", "http://foo.com/bar")
    mockResponse = new MockHttpServletResponse
  }

  describe("doFilter") {
    it("should pass an empty chain to the next filter when there is only one remaining") {
      val filterChain = new ReposeFilterChain(
        List(FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val request = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(request.capture(), same(mockResponse), argument.capture())
      request.getValue.getRequest shouldBe mockRequest
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain shouldBe empty
    }

    it("should pass the tail of the chain onto the next filter") {
      val filterChain = new ReposeFilterChain(
        List(
          FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null),
          FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val request = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(request.capture(), same(mockResponse), argument.capture())
      request.getValue.getRequest shouldBe mockRequest
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain should have size 1
    }

    it("should skip filters that don't pass the check") {
      val filterChain = new ReposeFilterChain(
        List(
          FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => false, null),
          FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val request = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(request.capture(), same(mockResponse), argument.capture())
      request.getValue.getRequest shouldBe mockRequest
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain shouldBe empty
    }

    it("should go to the original filter chain if it's empty") {
      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val request = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(originalChain).doFilter(request.capture(), same(mockResponse))
      request.getValue.getRequest shouldBe mockRequest
    }

    it("should skip the whole chain if the bypass uri is hit") {
      val filterChain = new ReposeFilterChain(
        List(
          FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => false, null),
          FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        Option(".*/bar"),
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val request = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(originalChain).doFilter(request.capture(), same(mockResponse))
      request.getValue.getRequest shouldBe mockRequest
    }

    it("should go into the the chain when the bypass uri isn't hit") {
      val filterChain = new ReposeFilterChain(
        List(
          FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null),
          FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        Option(".*/butts"),
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val request = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(mockFilter).doFilter(request.capture(), same(mockResponse), any(classOf[FilterChain]))
      request.getValue.getRequest shouldBe mockRequest
    }

    it("should not log out to intrafilter logging if the trace header is not present") {
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()
      Configurator.setLevel(ReposeFilterChain.IntrafilterLog.getName, Level.TRACE)

      val filterChain = new ReposeFilterChain(
        List(FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Intrafilter Request Log")) shouldBe empty
      messageList.filter(_.contains("Intrafilter Response Log")) shouldBe empty
    }

    it("should not log out to intrafilter logging if the log level is set too low") {
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()
      Configurator.setLevel(ReposeFilterChain.IntrafilterLog.getName, Level.INFO)

      MDC.put(TRACE_REQUEST, "true")

      val filterChain = new ReposeFilterChain(
        List(FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Intrafilter Request Log")) shouldBe empty
      messageList.filter(_.contains("Intrafilter Response Log")) shouldBe empty
    }

    it("should log out to intrafilter logging around a filter") {
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()
      Configurator.setLevel(ReposeFilterChain.IntrafilterLog.getName, Level.TRACE)

      MDC.put(TRACE_REQUEST, "true")

      val filterChain = new ReposeFilterChain(
        List(FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Intrafilter Request Log")) should have length 1
      messageList.filter(_.contains("Intrafilter Response Log")) should have length 1
    }

    it("should log out to intrafilter logging around the origin service") {
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()
      Configurator.setLevel(ReposeFilterChain.IntrafilterLog.getName, Level.TRACE)

      MDC.put(TRACE_REQUEST, "true")

      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Intrafilter Request Log")) should have length 1
      messageList.filter(_.contains("Intrafilter Response Log")) should have length 1
    }

    it("should report metrics around a filter") {
      val filterChain = new ReposeFilterChain(
        List(
          FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null),
          FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(timer).update(anyLong(), meq(TimeUnit.MILLISECONDS))
    }

    it("should report metrics around the origin service") {
      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(timer).update(anyLong(), meq(TimeUnit.MILLISECONDS))
    }

    it("should log filter timing metrics") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null),
                                                   FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)
      mockResponse.setCommitted(true)
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.find(_.startsWith("Filter foo spent")) should not be empty
    }

    it("should update the request in the HTTP logging context") {
      mockRequest.setAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT, loggingContext)

      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(loggingContext).setOutboundRequest(any[HttpServletRequest])
    }

    it("should not update the request in the HTTP logging context if the context is invalid") {
      mockRequest.setAttribute(CommonRequestAttributes.HTTP_LOGGING_CONTEXT, "not-a-context")

      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(loggingContext, never).setOutboundRequest(any[HttpServletRequest])
    }

    it("should not update the request in the HTTP logging context if the context is missing") {
      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(loggingContext, never).setOutboundRequest(any[HttpServletRequest])
    }

    it("should return a 500 when an exception occurs") {
      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)
      val message = "test exception"
      when(originalChain.doFilter(any[HttpServletRequest], any[HttpServletResponse])).thenThrow(new RuntimeException(message))

      filterChain.doFilter(mockRequest, mockResponse)

      mockResponse.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("should log if there was a an exception and the response was already committed") {
      val filterChain = new ReposeFilterChain(
        List.empty,
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)
      when(originalChain.doFilter(any[HttpServletRequest], any[HttpServletResponse])).thenThrow(new RuntimeException("test exception"))
      mockResponse.setCommitted(true)
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Exception thrown while processing the chain.")) should have length 1
    }

    it("should create a span with the filter name") {
      val filterChain = new ReposeFilterChain(
        List(
          FilterContext(mockFilter, fooFilterConfig, (request: HttpServletRequest) => true, null),
          FilterContext(mock[Filter], barFilterConfig, (request: HttpServletRequest) => true, null)),
        originalChain,
        None,
        Option(metricsRegistry),
        tracer)

      filterChain.doFilter(mockRequest, mockResponse)

      tracer.finishedSpans.asScala.loneElement.operationName() shouldBe "Filter foo"
    }
  }
}
