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

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.mockito.Matchers.same
import org.openrepose.powerfilter.ReposeFilterChain.FilterContext
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeFilterChainTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  var mockFilter: Filter =_
  var mockRequest: HttpServletRequest = _
  var mockResponse: HttpServletResponse = _
  var originalChain: FilterChain = _

  override protected def beforeEach(): Unit = {
    mockFilter = mock[Filter]
    mockRequest = mock[HttpServletRequest]
    mockResponse = mock[HttpServletResponse]
    originalChain = mock[FilterChain]
  }

  describe("doFilter") {
    it("should pass an empty chain to the next filter when there is only one remaining") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true)),
                                              originalChain)

      filterChain.doFilter(mockRequest, mockResponse)

      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), argument.capture())
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain shouldBe empty
    }

    it("should pass the tail of the chain onto the next filter") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true),
                                                   FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => true)),
                                              originalChain)

      filterChain.doFilter(mockRequest, mockResponse)

      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), argument.capture())
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain should have size 1
    }

    it("should skip filters that don't pass the check") {
      val filterChain = new ReposeFilterChain(List(FilterContext(mock[Filter], "bar", (request: HttpServletRequest) => false),
                                                   FilterContext(mockFilter, "foo", (request: HttpServletRequest) => true)),
                                              originalChain)

      filterChain.doFilter(mockRequest, mockResponse)

      val argument = ArgumentCaptor.forClass(classOf[FilterChain])
      verify(mockFilter).doFilter(same(mockRequest), same(mockResponse), argument.capture())
      argument.getValue.asInstanceOf[ReposeFilterChain].filterChain shouldBe empty
    }

    it("should go to the original filter chain if it's empty") {
      val filterChain = new ReposeFilterChain(List.empty, originalChain)

      filterChain.doFilter(mockRequest, mockResponse)

      verify(originalChain).doFilter(mockRequest, mockResponse)
    }
  }
}
