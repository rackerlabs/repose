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
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{Filter, FilterChain}
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.test.appender.ListAppender
import org.apache.logging.log4j.{Level, LogManager}
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, anyLong, same, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.openrepose.powerfilter.ReposeFilterChain.{FilterContext, TracingHeader}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ReposeFilterChainTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  var mockFilter: Filter =_
  var mockRequest: MockHttpServletRequest = _
  var mockResponse: MockHttpServletResponse = _
  var originalChain: FilterChain = _
  var metricsRegistry: MetricRegistry = _
  var timer: Timer = _

  override protected def beforeEach(): Unit = {
    mockFilter = mock[Filter]
    mockRequest = new MockHttpServletRequest("GET", "http://foo.com/bar")
    mockResponse = new MockHttpServletResponse
    originalChain = mock[FilterChain]
    metricsRegistry = mock[MetricRegistry]
    timer = mock[Timer]
    when(metricsRegistry.timer(any[String])).thenReturn(timer)
  }

  describe("doFilter") {
    it("should pass an empty chain to the next filter when there is only one remaining") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              None,
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), argument.capture())
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain shouldBe empty
    }

    it("should pass the tail of the chain onto the next filter") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true),
                                                   FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              None,
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), argument.capture())
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain should have size 1
    }

    it("should skip filters that don't pass the check") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => false),
                                                   FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              None,
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), argument.capture())
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain shouldBe empty
    }

    it("should go to the original filter chain if it's empty") {
      val filterChain = new ReposeFilterChain(List.empty, originalChain, None, metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(originalChain).doFilter(mockRequest, mockResponse)
    }

    it("should skip the whole chain if the bypass url is hit") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => false),
                                                   FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              Option(".*/bar"),
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(originalChain).doFilter(mockRequest, mockResponse)
    }

    it("should go into the the chain when the bypass url isn't hit") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true),
                                                   FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              Option(".*/butts"),
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), any(classOf[FilterChain]))
    }

    it("should log out to intrafilter logging around a filter") {
      val loggerContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = loggerContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
      listAppender.clear()
      Configurator.setLevel(ReposeFilterChain.IntrafilterLog.getName, Level.TRACE)

      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              None,
                                              metricsRegistry)

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

      val filterChain = new ReposeFilterChain(List.empty,
                                              originalChain,
                                              None,
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      val messageList = listAppender.getEvents.asScala.map(_.getMessage.getFormattedMessage)
      messageList.filter(_.contains("Intrafilter Request Log")) should have length 1
      messageList.filter(_.contains("Intrafilter Response Log")) should have length 1
    }

    it("should report metrics around a filter") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true),
                                                   FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              None,
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(timer).update(anyLong(), meq(TimeUnit.MILLISECONDS))
    }

    it("should report metrics around the origin service") {
      val filterChain = new ReposeFilterChain(List.empty,
                                              originalChain,
                                              None,
                                              metricsRegistry)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(timer).update(anyLong(), meq(TimeUnit.MILLISECONDS))
    }

    it("should add the metrics to the response when requested around a filter") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true),
                                                   FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => true)),
                                              originalChain,
                                              None,
                                              metricsRegistry)
      mockRequest.addHeader(TracingHeader, "true")

      filterChain.doFilter(mockRequest, mockResponse)

      mockResponse.getHeaderNames should contain ("X-foo-Time")
    }

    it("should add the metrics to the response when requested around the origin service") {
      val filterChain = new ReposeFilterChain(List.empty,
                                              originalChain,
                                              None,
                                              metricsRegistry)
      mockRequest.addHeader(TracingHeader, "true")

      filterChain.doFilter(mockRequest, mockResponse)

      mockResponse.getHeaderNames should contain ("X-origin-Time")
    }

    it("should not add the metrics to the response when requested and the response is already committed") {
      val filterChain = new ReposeFilterChain(List.empty,
                                              originalChain,
                                              None,
                                              metricsRegistry)
      mockRequest.addHeader(TracingHeader, "true")
      mockResponse.setCommitted(true)

      filterChain.doFilter(mockRequest, mockResponse)

      mockResponse.getHeaderNames should not contain "X-origin-Time"
    }
  }
}
