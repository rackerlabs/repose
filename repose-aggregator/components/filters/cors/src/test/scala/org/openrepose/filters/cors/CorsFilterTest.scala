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

import javax.servlet.FilterChain

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.scalatest.{Matchers, BeforeAndAfter, FunSpec}
import org.scalatest.junit.JUnitRunner
import org.springframework.mock.web.{MockHttpServletResponse, MockHttpServletRequest}

@RunWith(classOf[JUnitRunner])
class CorsFilterTest extends FunSpec with BeforeAndAfter with Matchers {

  val HttpMethods = List("OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT", "CUSTOM")

  var corsFilter: CorsFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _

  before {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock(classOf[FilterChain])

    corsFilter = new CorsFilter
  }

  describe("the doFilter method") {
    describe("when a non-CORS request is received") {
      HttpMethods.foreach { httpMethod =>
        it (s"should call the next filter in the filter chain for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          verify(filterChain).doFilter(servletRequest, servletResponse)
        }
      }

      HttpMethods.foreach { httpMethod =>
        it(s"should not add CORS specific headers for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString) shouldBe null
          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString) shouldBe null
          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString) shouldBe null
          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString) shouldBe null
          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString) shouldBe null
        }
      }

      HttpMethods.filter{_ != "OPTIONS"}.foreach { httpMethod =>
        it(s"should have 'Origin' in the Vary header for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CommonHttpHeader.VARY.toString) should contain theSameElementsAs List(CommonHttpHeader.ORIGIN.toString)
        }
      }

      it("should have the preflight request headers in the Vary header for HTTP method OPTIONS") {
        // given no request headers
        servletRequest.setMethod("OPTIONS")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CommonHttpHeader.VARY.toString) should contain theSameElementsAs List(
          CommonHttpHeader.ORIGIN.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString)
      }
    }
  }

}
