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
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.scalatest.{Matchers, BeforeAndAfter, FunSpec}
import org.scalatest.junit.JUnitRunner
import org.springframework.mock.web.{MockHttpServletResponse, MockHttpServletRequest}

import scala.collection.JavaConverters._

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

        it(s"should not have an HTTP status set for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)
          servletResponse.setStatus(-321)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus shouldBe -321  // verify unchanged
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

    describe("when a preflight request is received") {
      HttpMethods.foreach { requestMethod =>
        it(s"should return an HTTP status of 200 for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)
          servletResponse.setStatus(-321)  // since default value is 200 (the test success value)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus shouldBe 200
          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString) shouldBe null
        }

        it(s"should not call the next filter for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          verify(filterChain, never()).doFilter(servletRequest, servletResponse)
        }

        it(s"should not add actual request specific headers for HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString) shouldBe null
        }

        it(s"should have the Access-Control-Allow-Methods header set for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString) shouldEqual requestMethod
        }

        List("X-Auth-Token", "X-Panda, X-Unicorn", "Accept, User-Agent, X-Trans-Id").foreach { requestHeader =>
          it(s"should have the Access-Control-Allow-Headers header set for request HTTP method $requestMethod and request headers $requestHeader") {
            servletRequest.setMethod("OPTIONS")
            servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
            servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)
            servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString, requestHeader)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString) shouldEqual requestHeader
          }
        }

        it(s"should not have the Access-Control-Allow-Headers header set when none requested for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString) shouldBe null
        }

        it(s"should have the Access-Control-Allow-Credentials header set to true for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString) shouldEqual "true"
        }

        List("http://totally.allowed", "http://completely.legit:8080", "https://seriously.safe:8443").foreach { origin =>
          it(s"should have the Access-Control-Allow-Origin set to the Origin of the request for request HTTP method $requestMethod and origin $origin") {
            servletRequest.setMethod("OPTIONS")
            servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, origin)
            servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString) shouldEqual origin
          }
        }

        it(s"should have the Vary header correctly populated for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletRequest.addHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CommonHttpHeader.VARY.toString) should contain theSameElementsAs List(
            CommonHttpHeader.ORIGIN.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString)
        }
      }
    }

    describe("when an actual request is received") {
      HttpMethods.foreach { httpMethod =>
        it (s"should call the next filter in the filter chain for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          verify(filterChain).doFilter(servletRequest, servletResponse)
        }

        it(s"should not add preflight specific headers for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString) shouldBe null
          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString) shouldBe null
        }

        it(s"should not have an HTTP status set for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")
          servletResponse.setStatus(-321)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus shouldBe -321  // verify unchanged
        }

        List(
          List("X-Auth-Token"),
          List("X-Auth-Token", "X-Trans-Id"),
          List("X-Trans-Id", "Content-Type", "X-Panda", "X-OMG-Ponies")
        ).foreach { responseHeaders =>
          it(s"should include the response headers in Access-Control-Expose-Headers for HTTP method $httpMethod and headers $responseHeaders") {
            servletRequest.setMethod(httpMethod)
            servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")

            // only add the headers to the response when the filterchain doFilter method is called
            doAnswer(new Answer[Void]() {
              def answer(invocation: InvocationOnMock): Void = {
                responseHeaders.foreach(servletResponse.addHeader(_, "totally legit value"))
                null
              }
            }).when(filterChain).doFilter(servletRequest, servletResponse)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            // Access-Control-Expose-Headers should have all of the response headers in it except for itself
            servletResponse.getHeaders(CommonHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString) should contain theSameElementsAs
              servletResponse.getHeaderNames.asScala.filter{_ != CommonHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString}
          }
        }

        it(s"should have the Access-Control-Allow-Credentials header set to true for request HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString) shouldEqual "true"
        }

        List("http://totally.allowed", "http://completely.legit:8080", "https://seriously.safe:8443").foreach { origin =>
          it(s"should have the Access-Control-Allow-Origin set to the Origin of the request for request HTTP method $httpMethod and origin $origin") {
            servletRequest.setMethod(httpMethod)
            servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, origin)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            servletResponse.getHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString) shouldEqual origin
          }
        }
      }

      HttpMethods.filter{_ != "OPTIONS"}.foreach { httpMethod =>
        it(s"should have 'Origin' in the Vary header for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CommonHttpHeader.VARY.toString) should contain theSameElementsAs List(CommonHttpHeader.ORIGIN.toString)
        }
      }

      it("should have the preflight request headers in the Vary header for HTTP method OPTIONS") {
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CommonHttpHeader.ORIGIN.toString, "http://totally.allowed")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CommonHttpHeader.VARY.toString) should contain theSameElementsAs List(
          CommonHttpHeader.ORIGIN.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString)
      }
    }
  }

}
