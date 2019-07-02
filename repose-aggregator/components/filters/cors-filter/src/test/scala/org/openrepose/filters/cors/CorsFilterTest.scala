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
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.HttpHeaders

import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.http.{CommonHttpHeader, CorsHttpHeader}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.filters.cors.config.Origins.Origin
import org.openrepose.filters.cors.config._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class CorsFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import CorsFilterTest._

  val HttpMethods = List("OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT", "CUSTOM")

  var corsFilter: CorsFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach(): Unit = {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock[FilterChain]

    // unless we're specifically testing the same-origin logic, assume Host should not match the Origin
    servletRequest.setScheme("http")
    servletRequest.setServerName("www.does.not.match.origin.org")
    servletRequest.setServerPort(8080)

    corsFilter = new CorsFilter(null)
    allowAllOriginsAndMethods()
  }

  describe("the doFilter method") {
    describe("when a non-CORS request is received") {
      HttpMethods foreach { httpMethod =>
        it(s"should call the next filter in the filter chain for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          verify(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), any(classOf[HttpServletResponseWrapper]))
        }

        it(s"should not add CORS specific headers for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe null
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) shouldBe null
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS) shouldBe null
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) shouldBe null
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS) shouldBe null
        }

        it(s"should not have an HTTP status set for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)
          servletResponse.setStatus(-321)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus shouldBe -321 // verify unchanged
        }
      }

      HttpMethods.filterNot(_ == "OPTIONS") foreach { httpMethod =>
        it(s"should have 'Origin' in the Vary header for HTTP method $httpMethod") {
          // given no request headers
          servletRequest.setMethod(httpMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(HttpHeaders.VARY) should contain theSameElementsAs List[String](CorsHttpHeader.ORIGIN)
        }
      }

      it("should have the preflight request headers in the Vary header for HTTP method OPTIONS") {
        // given no request headers
        servletRequest.setMethod("OPTIONS")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(HttpHeaders.VARY) should contain theSameElementsAs List[String](
          CorsHttpHeader.ORIGIN, CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
      }
    }

    describe("when a preflight request is received") {
      HttpMethods foreach { requestMethod =>
        it(s"should return an HTTP status of 200 for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)
          servletResponse.setStatus(-321) // since default value is 200 (the test success value)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus shouldBe 200
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS) shouldBe null
        }

        it(s"should not call the next filter for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          verify(filterChain, never()).doFilter(any(), any())
        }

        it(s"should not add actual request specific headers for HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS) shouldBe null
        }

        it(s"should have the Access-Control-Allow-Methods header set for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should not be null
          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
        }

        List(
          (List("x-auth-token"), List("x-auth-token")),
          (List("x-panda, x-unicorn"), List("x-panda", "x-unicorn")),
          (List("x-cupcake", "x-pineapple"), List("x-cupcake", "x-pineapple")),
          (List("accept, user-agent, x-trans-id"), List("accept", "user-agent", "x-trans-id")),
          (List("x-one, x-two", "x-three"), List("x-one", "x-two", "x-three")),
          (List("x-red", "x-green", "x-blue"), List("x-red", "x-green", "x-blue"))
        ) foreach { case (requestHeaders, expectedAllowedHeaders) =>
          it(s"should have the Access-Control-Allow-Headers header set to $expectedAllowedHeaders for request HTTP method $requestMethod and request headers $requestHeaders") {
            servletRequest.setMethod("OPTIONS")
            servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
            servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)
            requestHeaders foreach (servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, _))

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS) should have size 1
            servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).split(",") should contain theSameElementsAs expectedAllowedHeaders
          }
        }

        it(s"should not have the Access-Control-Allow-Headers header set when none requested for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS) shouldBe null
        }

        it(s"should have the Access-Control-Allow-Credentials header set to true for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) shouldEqual "true"
        }

        List("http://totally.allowed", "http://completely.legit:8080", "https://seriously.safe:8443") foreach { origin =>
          it(s"should have the Access-Control-Allow-Origin set to the Origin of the request for request HTTP method $requestMethod and origin $origin") {
            servletRequest.setMethod("OPTIONS")
            servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)
            servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldEqual origin
          }
        }

        it(s"should have the Vary header correctly populated for request HTTP method $requestMethod") {
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, requestMethod)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(HttpHeaders.VARY) should contain theSameElementsAs List[String](
            CorsHttpHeader.ORIGIN, CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
        }
      }
    }

    describe("when an actual request is received") {
      HttpMethods foreach { httpMethod =>
        it(s"should call the next filter in the filter chain for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          verify(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), any(classOf[HttpServletResponseWrapper]))
        }

        it(s"should not add preflight specific headers for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) shouldBe null
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS) shouldBe null
        }

        it(s"should not have an HTTP status set for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletResponse.setStatus(-321)

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus shouldBe -321 // verify unchanged
        }

        List(
          List("X-Auth-Token"),
          List("X-Auth-Token", "X-Trans-Id"),
          List("X-Trans-Id", "Content-Type", "X-Panda", "X-OMG-Ponies")
        ) foreach { responseHeaders =>
          it(s"should include the response headers in Access-Control-Expose-Headers for HTTP method $httpMethod and headers $responseHeaders") {
            servletRequest.setMethod(httpMethod)
            servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")

            // only add the headers to the response when the filterChain doFilter method is called
            doAnswer(new Answer[Void]() {
              def answer(invocation: InvocationOnMock): Void = {
                responseHeaders foreach (servletResponse.addHeader(_, "totally legit value"))
                null
              }
            }).when(filterChain).doFilter(servletRequest, servletResponse)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            // Access-Control-Expose-Headers should have all of the response headers in it except for itself and the Vary header
            servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).toLowerCase.split(",") should contain theSameElementsAs
              servletResponse.getHeaderNames.asScala.filter { headerName =>
                headerName != CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS &&
                  headerName != HttpHeaders.VARY
              }.map(_.toLowerCase)
            servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS) should have size 1
          }
        }

        it(s"should have the Access-Control-Allow-Credentials header set to true for request HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) shouldEqual "true"
        }

        List("http://totally.allowed", "http://completely.legit:8080", "https://seriously.safe:8443") foreach { origin =>
          it(s"should have the Access-Control-Allow-Origin set to the Origin of the request for request HTTP method $httpMethod and origin $origin") {
            servletRequest.setMethod(httpMethod)
            servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)

            corsFilter.doFilter(servletRequest, servletResponse, filterChain)

            servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldEqual origin
          }
        }
      }

      HttpMethods.filterNot(_ == "OPTIONS") foreach { httpMethod =>
        it(s"should have 'Origin' in the Vary header for HTTP method $httpMethod") {
          servletRequest.setMethod(httpMethod)
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(HttpHeaders.VARY) should contain theSameElementsAs List[String](CorsHttpHeader.ORIGIN)
        }
      }

      it("should have the preflight request headers in the Vary header for HTTP method OPTIONS") {
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(HttpHeaders.VARY) should contain theSameElementsAs List[String](
          CorsHttpHeader.ORIGIN, CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD)
      }
    }

    describe("when origin filtering") {
      it("should allow a preflight request with a specific origin") {
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletResponse.setStatus(-321) // since default value is 200 (the test success value)

        val configOrigin = new Origin
        configOrigin.setValue("http://totally.allowed")
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe 200 // preflight success
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe "http://totally.allowed"
      }

      it("should allow a preflight request with a regex matched origin") {
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://good.enough.com:8080")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletResponse.setStatus(-321) // since default value is 200 (the test success value)

        val configOrigin = new Origin
        configOrigin.setValue("http://.*good.enough.*")
        configOrigin.setRegex(true)
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe 200 // preflight success
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe "http://good.enough.com:8080"
      }

      it("should deny a preflight request with an unmatched origin") {
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://not.going.to.work:9000")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletResponse.setStatus(-321) // since default value is 200 (the test success value)

        val configOrigin = new Origin
        configOrigin.setValue("NOPE")
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe 403
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe null
      }

      it("should allow an actual request with a specific origin") {
        servletRequest.setMethod("GET")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://let.me.in:8000")
        servletResponse.setStatus(-321)

        val configOrigin = new Origin
        configOrigin.setValue("http://let.me.in:8000")
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe -321 // verify unchanged
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe "http://let.me.in:8000"
      }

      it("should allow an actual request with a regex matched origin") {
        servletRequest.setMethod("GET")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "https://you.can.trust.me:8443")
        servletResponse.setStatus(-321)

        val configOrigin = new Origin
        configOrigin.setValue("https://.*trust.*443")
        configOrigin.setRegex(true)
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe -321 // verify unchanged
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe "https://you.can.trust.me:8443"
      }

      it("should deny an actual request with an unmatched origin") {
        servletRequest.setMethod("GET")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://no.way.bro:80")
        servletResponse.setStatus(-321)

        val configOrigin = new Origin
        configOrigin.setValue("NOPE")
        configOrigin.setRegex(true)
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe 403
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe null
      }

      it("should allow a non-CORS request that does not have an origin header") {
        servletRequest.setMethod("GET")
        servletResponse.setStatus(-321)

        val configOrigin = new Origin
        configOrigin.setValue("NOPE")
        setConfiguredAllowedOriginsTo(List(configOrigin))

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getStatus shouldBe -321 // verify unchanged
      }
    }

    describe("when specifying which HTTP methods are allowed for a resource") {
      HttpMethods foreach { httpMethod =>
        it(s"should permit HTTP method $httpMethod when it is globally allowed in config") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List(httpMethod), List()))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain(httpMethod)
        }

        it(s"should not permit HTTP method $httpMethod when it is not globally allowed in config") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("TRANSMUTE"), List()))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should not contain httpMethod
        }

        it(s"should permit HTTP method $httpMethod when it is configured for the root resource") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("NOTHING"), List(("/.*", List(httpMethod)))))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain(httpMethod)
        }

        it(s"should not permit HTTP method $httpMethod when it is not configured and a root resource allows something else") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("TRANSMUTE"), List(("/.*", List("DESTROY")))))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should not contain httpMethod
        }

        it(s"should not permit HTTP method $httpMethod when a specific child resource eclipses the root resource permission") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("TRANSMUTE"), List(("/servers", List("CREATE")), ("/.*", List(httpMethod)))))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/servers")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should not contain httpMethod
        }

        it(s"should permit HTTP method $httpMethod when a specific child resource does not but global config does") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List(httpMethod), List(("/servers", List("TRANSMUTE")), ("/.*", List("DESTROY")))))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/servers")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain(httpMethod)
        }

        it(s"should permit HTTP method $httpMethod when a specific child resource allows it") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("STARE"), List(("/servers", List(httpMethod)), ("/.*", List("DESTROY")))))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/servers")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
          servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain(httpMethod)
        }

        it(s"should return a 403 when the requested method is not allowed for method $httpMethod") {
          corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("POKE"), List()))
          servletRequest.setMethod("OPTIONS")
          servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
          servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, httpMethod)
          servletRequest.setRequestURI("/servers")

          corsFilter.doFilter(servletRequest, servletResponse, filterChain)

          servletResponse.getStatus == 403
        }
      }

      it("should permit multiple HTTP methods specified in global config") {
        corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("GET", "POST", "PUT", "DELETE"), List()))
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletRequest.setRequestURI("/")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain theSameElementsAs List("GET", "POST", "PUT", "DELETE")
      }

      it("should permit multiple HTTP methods specified in both global config and a specific resource") {
        corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("GET", "POST"), List(("/players", List("PUT", "DELETE")))))
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletRequest.setRequestURI("/players")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain theSameElementsAs List("GET", "POST", "PUT", "DELETE")
      }

      it("should permit multiple HTTP methods specified in both global config and a specific root resource") {
        corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("GET", "POST"), List(("/.*", List("PUT", "PATCH")))))
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletRequest.setRequestURI("/players")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain theSameElementsAs List("GET", "POST", "PUT", "PATCH")
      }

      it("should be able to handle a path param with a configured resource path specified with regex") {
        corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("GET"), List(("/players/[^/]+/achievements", List("POST", "PUT", "PATCH")))))
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletRequest.setRequestURI("/players/bob_loblaw/achievements")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain theSameElementsAs List("GET", "POST", "PUT", "PATCH")
      }

      it("should permit multiple HTTP methods specified in both global config and a specific resource with no methods") {
        corsFilter.configurationUpdated(createCorsConfig(List(".*"), List("GET", "POST"), List(("/players", List()))))
        servletRequest.setMethod("OPTIONS")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://totally.allowed")
        servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        servletRequest.setRequestURI("/players")

        corsFilter.doFilter(servletRequest, servletResponse, filterChain)

        servletResponse.getHeaders(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS) should have size 1
        servletResponse.getHeader(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).split(",") should contain theSameElementsAs List("GET", "POST")
      }
    }
  }

  describe("the configurationUpdated method") {
    for (
      origins <- List(
        List("http://legit.com:8080"),
        List("http://potato.com", "https://panda.com:8443", "pancakes.and.bacon"));
      methods <- List(
        List(),
        List("GET"),
        List("OPTIONS", "POST", "PATCH"));
      resources <- List(
        List(),
        List(("/v1/.*", List())),
        List(("/v1/.*", List("GET", "PUT"))),
        List(("/v1/.*", List("GET", "PUT")), ("/v2/.*", List("DELETE"))))
    ) {
      it(s"should be able to load configuration for origins $origins, methods $methods, resources $resources") {
        corsFilter.configurationUpdated(createCorsConfig(origins, methods, resources))
      }
    }
  }

  describe("the getHostUri method") {
    Seq(("http", 80), ("https", 443)) foreach { case (scheme, port) =>
      Seq("openrepose.org", "10.8.4.4", "zombo.com", "rackspace.com") foreach { serverName =>
        it(s"should be able to parse the request scheme '$scheme', serverName '$serverName', and port '$port' into a URI") {
          servletRequest.setScheme(scheme)
          servletRequest.setServerName(serverName)
          servletRequest.setServerPort(-1)

          val uri = corsFilter.getHostUri(servletRequest)

          uri.getScheme shouldBe scheme
          uri.getHost shouldBe serverName
          uri.getPort shouldBe port
        }

        Seq(8080, 8443, 7777) foreach { expectedPort =>
          it(s"should be able to parse the request scheme '$scheme', serverName '$serverName', and port '$expectedPort' into a URI") {
            servletRequest.setScheme(scheme)
            servletRequest.setServerName(serverName)
            servletRequest.setServerPort(expectedPort)

            val uri = corsFilter.getHostUri(servletRequest)

            uri.getScheme shouldBe scheme
            uri.getHost shouldBe serverName
            uri.getPort shouldBe expectedPort
          }
        }
      }

      Seq(
        "[2001:db8:cafe::17]",
        "[2001:db8:cafe:0:0:0:0:17]",
        "[2001:0Db8:Cafe:0000:0000:0000:0000:0017]",
        "[2001:0DB8:CAFE:0000:0000:0000:0000:0017]") foreach { serverName =>
        it(s"should be able to parse the request scheme '$scheme', serverName '$serverName', and port '$port' into a URI") {
          servletRequest.setScheme(scheme)
          servletRequest.setServerName(serverName)
          servletRequest.setServerPort(-1)

          val uri = corsFilter.getHostUri(servletRequest)

          uri.getScheme shouldBe scheme
          uri.getHost shouldBe "[2001:db8:cafe::17]"
          uri.getPort shouldBe port
        }
      }
    }

    it("should parse the X-Forwarded-Host header if it's available instead of using the Host header") {
      servletRequest.setScheme("https")
      servletRequest.setServerName("garbage.value")
      servletRequest.setServerPort(9999999)
      servletRequest.addHeader(CommonHttpHeader.X_FORWARDED_HOST, "expected.host.com:8443")

      val uri = corsFilter.getHostUri(servletRequest)

      uri.getScheme shouldBe "https"
      uri.getHost shouldBe "expected.host.com"
      uri.getPort shouldBe 8443
    }

    it("should use the Host header if the X-Forwarded-Host header could not be parsed") {
      // The X-Forwarded-Host header is not backed by an official specification, so if the filter ever has trouble
      // parsing the value, the filter should use the Host header instead.
      val scheme = "http"
      val serverName = "openrepose.org"
      val port = 80
      servletRequest.setScheme(scheme)
      servletRequest.setServerName(serverName)
      servletRequest.setServerPort(port)
      servletRequest.addHeader(CommonHttpHeader.X_FORWARDED_HOST, "not.zombo.com:abc")

      val uri = corsFilter.getHostUri(servletRequest)

      uri.getScheme shouldBe scheme
      uri.getHost shouldBe serverName
      uri.getPort shouldBe port
    }

    List(
      ("zombo.com", 8080, List("zombo.com:8080")),
      ("zombo.com", 5656, List("zombo.com:5656,not.a.match.com:7474")),
      ("zombo.com", 3434, List("zombo.com:3434", "not.a.match.com:7474")),
      ("totally.a.match.com", 7474, List("totally.a.match.com:7474,zombo.com:8080")),
      ("totally.a.match.com", 8989, List("totally.a.match.com:8989", "zombo.com:8080")),
      ("zombo.com", 4747, List("zombo.com:4747,not.a.match.com:7474", "another.failed.match:5555")),
      ("zombo.com", 1111, List("zombo.com:1111", "not.a.match.com:7474", "another.failed.match:5555")),
      ("is.a.match.com", 222, List("is.a.match.com:222,zombo.com:8080", "another.failed.match:5555")),
      ("is.a.match.com", 333, List("is.a.match.com:333", "zombo.com:8080", "another.failed.match:5555")),
      ("is.a.match.com", 444, List("is.a.match.com:444,another.failed.match:invalid.port", "zombo.com:8080")),
      ("is.a.match.com", 555, List("is.a.match.com:555", "another.failed.match:5555", "zombo.com:invalid.port"))
    ) foreach { case (expectedHost, expectedPort, forwardedHostHeaders) =>
      it(s"should only parse the first X-Forwarded-Host header in '$forwardedHostHeaders'") {
        servletRequest.setScheme("http")
        servletRequest.setServerName(null)
        servletRequest.setServerPort(-1)
        forwardedHostHeaders.foreach(servletRequest.addHeader(CommonHttpHeader.X_FORWARDED_HOST, _))

        val uri = corsFilter.getHostUri(servletRequest)

        uri.getScheme shouldBe "http"
        uri.getHost shouldBe expectedHost
        uri.getPort shouldBe expectedPort
      }
    }
  }

  describe("the determineRequestType method") {
    import CorsFilter._

    it("should return same-origin result when Origin header is not present in request") {
      servletRequest.setScheme(null)
      servletRequest.setServerName(null)
      servletRequest.setServerPort(-1)

      corsFilter.determineRequestType(servletRequest) shouldBe NonCorsRequest
    }

    List(
      ("http", "2001:db8:cafe::34", 4444, "http://[2001:db8:cafe::34]:4444"),
      ("http", "10.8.4.4", 80, "http://10.8.4.4:80"),
      ("https", "openrepose.org", 443, "https://openrepose.org:443")
    ) foreach { case (scheme, serverName, port, origin) =>
      it(s"should return CORS result when Origin '$origin' does not match forwardedHost despite matching the other values") {
        servletRequest.setScheme(scheme)
        servletRequest.setServerName(serverName)
        servletRequest.setServerPort(port)
        servletRequest.addHeader(CommonHttpHeader.X_FORWARDED_HOST, "never.match.com:7070")
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)

        corsFilter.determineRequestType(servletRequest) shouldBe ActualCorsRequest(origin)
      }
    }

    it("should return Preflight result when Origin and preflight header Access-Control-Request-Method is present in request despite the Host matching the Origin") {
      // A CORS preflight header should never be in a same-origin request, so be sure the filter does not bother to
      // check the Host header in this scenario.
      servletRequest.setScheme("http")
      servletRequest.setServerName("openrepose.org")
      servletRequest.setServerPort(80)
      servletRequest.setMethod("OPTIONS")
      servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://openrepose.org:80")
      servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")

      corsFilter.determineRequestType(servletRequest) shouldBe PreflightCorsRequest("http://openrepose.org:80", "PATCH")
    }

    it("should return Preflight result without throwing an exception when Origin and preflight header Access-Control-Request-Method is present in request when Origin contains malformed data") {
      // A CORS preflight header should never be in a same-origin request, so be sure the filter does not bother to
      // parse the Origin header in this scenario.
      servletRequest.setScheme("http")
      servletRequest.setServerName("openrepose.org")
      servletRequest.setServerPort(80)
      servletRequest.setMethod("OPTIONS")
      servletRequest.addHeader(CorsHttpHeader.ORIGIN, "http://openrepose.org:not_a_number")
      servletRequest.addHeader(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "PUT")

      corsFilter.determineRequestType(servletRequest) shouldBe PreflightCorsRequest("http://openrepose.org:not_a_number", "PUT")
    }

    List(
      // host/origin comparison should be case insensitive
      ("http", "www.openrepose.org", 9191, "http://www.openrepose.org:9191"),
      ("http", "www.openrepose.ORG", 9191, "http://WWW.openrepose.org:9191"),
      ("http", "www.openrepose.org", 9191, "http://www.OPENREPOSE.org:9191"),
      ("http", "WWW.openREPOSE.org", 9191, "http://Www.Openrepose.Org:9191"),
      ("https", "www.openrepose.org", 9191, "https://www.openrepose.org:9191"),
      ("https", "www.openrepose.ORG", 9191, "https://WWW.openrepose.org:9191"),
      ("https", "www.openREPOSE.org", 9191, "https://www.OPENREPOSE.org:9191"),
      ("https", "WWW.openrepose.org", 9191, "https://Www.Openrepose.Org:9191"),
      // scheme/origin comparison should be case insensitive
      ("http", "www.openrepose.org", 9191, "HTTP://www.openrepose.org:9191"),
      ("http", "www.openrepose.org", 9191, "Http://www.openrepose.org:9191"),
      ("https", "www.openrepose.org", 9191, "HTTPS://www.openrepose.org:9191"),
      ("https", "www.openrepose.org", 9191, "Https://www.openrepose.org:9191"),
      // default ports should be supported
      ("http", "www.openrepose.org", -1, "http://www.openrepose.org"),
      ("http", "www.openrepose.org", -1, "http://www.openrepose.org:"),
      ("http", "www.openrepose.org", -1, "http://www.openrepose.org:80"),
      ("http", "www.openrepose.org", 80, "http://www.openrepose.org"),
      ("http", "www.openrepose.org", 80, "http://www.openrepose.org:"),
      ("http", "www.openrepose.org", 80, "http://www.openrepose.org:80"),
      ("https", "www.openrepose.org", -1, "https://www.openrepose.org"),
      ("https", "www.openrepose.org", -1, "https://www.openrepose.org:"),
      ("https", "www.openrepose.org", -1, "https://www.openrepose.org:443"),
      ("https", "www.openrepose.org", 443, "https://www.openrepose.org"),
      ("https", "www.openrepose.org", 443, "https://www.openrepose.org:"),
      ("https", "www.openrepose.org", 443, "https://www.openrepose.org:443"),
      // IPv4, default ports should be supported
      ("http", "192.30.252.153", -1, "http://192.30.252.153"),
      ("http", "192.30.252.153", -1, "http://192.30.252.153:"),
      ("http", "192.30.252.153", -1, "http://192.30.252.153:80"),
      ("http", "192.30.252.153", 80, "http://192.30.252.153"),
      ("http", "192.30.252.153", 80, "http://192.30.252.153:"),
      ("http", "192.30.252.153", 80, "http://192.30.252.153:80"),
      ("https", "192.30.252.153", -1, "https://192.30.252.153"),
      ("https", "192.30.252.153", -1, "https://192.30.252.153:"),
      ("https", "192.30.252.153", -1, "https://192.30.252.153:443"),
      ("https", "192.30.252.153", 443, "https://192.30.252.153"),
      ("https", "192.30.252.153", 443, "https://192.30.252.153:"),
      ("https", "192.30.252.153", 443, "https://192.30.252.153:443"),
      // IPv6, host/origin comparison should support canonical and verbose formatting
      ("http", "2001:db8::beef:28", 8888, "http://[2001:db8::beef:28]:8888"),
      ("http", "2001:db8::beef:28", 8888, "http://[2001:db8::BEEF:28]:8888"),
      ("http", "2001:db8::BEEF:28", 8888, "http://[2001:db8::beef:28]:8888"),
      ("http", "2001:db8::beef:28", 8888, "http://[2001:db8:0:0:0:0:beef:28]:8888"),
      ("http", "2001:db8::beef:28", 8888, "http://[2001:0db8:0000:0000:0000:0000:beef:0028]:8888"),
      ("http", "2001:db8:0:0:0:0:beef:28", 8888, "http://[2001:db8::beef:28]:8888"),
      ("http", "2001:0db8:0000:0000:0000:0000:beef:0028", 8888, "http://[2001:db8::beef:28]:8888"),
      ("http", "2001:0db8:0000:0000:0000:0000:beef:0028", 8888, "http://[2001:0db8:0000:0000:0000:0000:beef:0028]:8888"),
      ("http", "::2001:db8:beef:28", 8888, "http://[::2001:db8:beef:28]:8888"),
      ("http", "::2001:db8:beef:28", 8888, "http://[0:0:0:0:2001:db8:beef:28]:8888"),
      ("http", "::2001:db8:beef:28", 8888, "http://[0000:0000:0000:0000:2001:0db8:beef:0028]:8888"),
      ("http", "0:0:0:0:2001:db8:beef:28", 8888, "http://[::2001:db8:beef:28]:8888"),
      ("http", "0000:0000:0000:0000:2001:0db8:beef:0028", 8888, "http://[::2001:db8:beef:28]:8888"),
      ("http", "2001:db8:beef:28::", 8888, "http://[2001:db8:beef:28::]:8888"),
      ("http", "2001:db8:beef:28::", 8888, "http://[2001:db8:beef:28:0:0:0:0]:8888"),
      ("http", "2001:db8:beef:28::", 8888, "http://[2001:0db8:beef:0028:0000:0000:0000:0000]:8888"),
      ("http", "2001:db8:beef:28:0:0:0:0", 8888, "http://[2001:db8:beef:28::]:8888"),
      ("http", "2001:0db8:beef:0028:0000:0000:0000:0000", 8888, "http://[2001:db8:beef:28::]:8888"),
      ("https", "2001:db8::beef:28", 8888, "https://[2001:db8::beef:28]:8888"),
      // IPv6, default port should be supported (none specified)
      ("http", "2001:db8::beef:28", -1, "http://[2001:db8::beef:28]"),
      ("http", "2001:db8:beef:28::", -1, "http://[2001:db8:beef:28::]"),
      ("http", "::2001:db8:beef:28", -1, "http://[::2001:db8:beef:28]"),
      ("http", "2001:db8:0:0:0:0:beef:28", -1, "http://[2001:db8:0:0:0:0:beef:28]"),
      ("http", "2001:0db8:0000:0000:0000:0000:beef:0028", -1, "http://[2001:0db8:0000:0000:0000:0000:beef:0028]"),
      ("https", "2001:db8::beef:28", -1, "https://[2001:db8::beef:28]"),
      ("https", "2001:db8:beef:28::", -1, "https://[2001:db8:beef:28::]"),
      ("https", "::2001:db8:beef:28", -1, "https://[::2001:db8:beef:28]"),
      ("https", "2001:db8:0:0:0:0:beef:28", -1, "https://[2001:db8:0:0:0:0:beef:28]"),
      ("https", "2001:0db8:0000:0000:0000:0000:beef:0028", -1, "https://[2001:0db8:0000:0000:0000:0000:beef:0028]"),
      // IPv6, default port should be supported (none specified, colon in the origin)
      ("http", "2001:db8::beef:28", -1, "http://[2001:db8::beef:28]:"),
      ("http", "2001:db8:beef:28::", -1, "http://[2001:db8:beef:28::]:"),
      ("http", "::2001:db8:beef:28", -1, "http://[::2001:db8:beef:28]:"),
      ("http", "2001:db8:0:0:0:0:beef:28", -1, "http://[2001:db8:0:0:0:0:beef:28]:"),
      ("http", "2001:0db8:0000:0000:0000:0000:beef:0028", -1, "http://[2001:0db8:0000:0000:0000:0000:beef:0028]:"),
      ("https", "2001:db8::beef:28", -1, "https://[2001:db8::beef:28]:"),
      ("https", "2001:db8:beef:28::", -1, "https://[2001:db8:beef:28::]:"),
      ("https", "::2001:db8:beef:28", -1, "https://[::2001:db8:beef:28]:"),
      ("https", "2001:db8:0:0:0:0:beef:28", -1, "https://[2001:db8:0:0:0:0:beef:28]:"),
      ("https", "2001:0db8:0000:0000:0000:0000:beef:0028", -1, "https://[2001:0db8:0000:0000:0000:0000:beef:0028]:"),
      // IPv6, default port should be supported (specified in one but not the other)
      ("http", "2001:db8::beef:28", 80, "http://[2001:db8::beef:28]"),
      ("http", "2001:db8::beef:28", -1, "http://[2001:db8::beef:28]:80"),
      ("https", "2001:db8::beef:28", 443, "https://[2001:db8::beef:28]"),
      ("https", "2001:db8::beef:28", -1, "https://[2001:db8::beef:28]:443")
    ) foreach { case (scheme, serverName, port, origin) =>
      it(s"should return same-origin result when Origin '$origin' matches the scheme '$scheme', serverName '$serverName', and port '$port'") {
        servletRequest.setScheme(scheme)
        servletRequest.setServerName(serverName)
        servletRequest.setServerPort(port)
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)

        corsFilter.determineRequestType(servletRequest) shouldBe NonCorsRequest
      }

      val forwardedPort = if (port != -1) s":$port" else ""
      val forwardedHost = if (serverName.contains(":")) s"[$serverName]$forwardedPort" else s"$serverName$forwardedPort"
      it(s"should return same-origin result when Origin '$origin' matches the scheme '$scheme' and forwardedHost '$forwardedHost'") {
        servletRequest.setScheme(scheme)
        servletRequest.setServerName(null)
        servletRequest.setServerPort(-1)
        servletRequest.addHeader(CommonHttpHeader.X_FORWARDED_HOST, forwardedHost)
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)

        corsFilter.determineRequestType(servletRequest) shouldBe NonCorsRequest
      }
    }

    List(
      // different subdomains are considered different origins
      ("http", "www.zombo.com", 4242, "http://zombo.com:4242"),
      ("http", "subdomain.zombo.com", 4242, "http://zombo.com:4242"),
      ("http", "dfw.zombo.com", 4242, "http://lon.zombo.com:4242"),
      ("https", "www.zombo.com", 4242, "https://zombo.com:4242"),
      ("https", "subdomain.zombo.com", 4242, "https://zombo.com:4242"),
      ("https", "dfw.zombo.com", 4242, "https://lon.zombo.com:4242"),
      // different ports are considered different origins
      ("http", "zombo.com", 1111, "http://zombo.com:2222"),
      ("http", "zombo.com", -1, "http://zombo.com:3333"),
      ("http", "zombo.com", -1, "http://zombo.com:443"),
      ("https", "zombo.com", 1111, "https://zombo.com:2222"),
      ("https", "zombo.com", -1, "https://zombo.com:3333"),
      ("https", "zombo.com", -1, "https://zombo.com:80"),
      // different host names and TLDs are considered different origins
      ("http", "limited.com", 4242, "http://zombo.com:4242"),
      ("http", "zombo.org", 4242, "http://zombo.com:4242"),
      ("https", "limited.com", 4242, "https://zombo.com:4242"),
      ("https", "zombo.org", 4242, "https://zombo.com:4242"),
      // different schemes are considered different origins
      ("http", "zombo.com", 4242, "https://zombo.com:4242"),
      ("https", "zombo.com", 4242, "http://zombo.com:4242")
    ) foreach { case (scheme, serverName, port, origin) =>
      it(s"should return CORS result when Origin '$origin' does not match the scheme '$scheme', serverName '$serverName', or port '$port'") {
        servletRequest.setScheme(scheme)
        servletRequest.setServerName(serverName)
        servletRequest.setServerPort(port)
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)

        corsFilter.determineRequestType(servletRequest) shouldBe ActualCorsRequest(origin)
      }

      val forwardedPort = if (port != -1) s":$port" else ""
      val forwardedHost = if (serverName.contains(":")) s"[$serverName]$forwardedPort" else s"$serverName$forwardedPort"
      it(s"should return CORS result when Origin '$origin' does not match the scheme '$scheme' and forwardedHost '$forwardedHost'") {
        servletRequest.setScheme(scheme)
        servletRequest.setServerName(null)
        servletRequest.setServerPort(-1)
        servletRequest.addHeader(CommonHttpHeader.X_FORWARDED_HOST, forwardedHost)
        servletRequest.addHeader(CorsHttpHeader.ORIGIN, origin)

        corsFilter.determineRequestType(servletRequest) shouldBe ActualCorsRequest(origin)
      }
    }
  }

  def createCorsConfig(allowedOrigins: List[String],
                       allowedMethods: List[String],
                       resources: List[(String, List[String])]): CorsConfig = {
    val config = new CorsConfig

    val configOrigins = new Origins
    configOrigins.getOrigin.addAll(allowedOrigins.map { value =>
      val origin = new Origin
      origin.setValue(value)
      origin.setRegex(true)
      origin
    }.asJava)
    config.setAllowedOrigins(configOrigins)

    // leave the list of methods null if there's nothing to configure
    if (allowedMethods.nonEmpty) {
      val configMethods = new Methods
      configMethods.getMethod.addAll(allowedMethods.asJava)
      config.setAllowedMethods(configMethods)
    }

    // leave the list of resources null if there's nothing to configure
    if (resources.nonEmpty) {
      val configResources = new Resources
      configResources.getResource.addAll(resources.map { case (path, resourceAllowedMethods) =>
        val configResource = new Resource
        configResource.setPath(path)

        // leave the list of methods null if there's nothing to configure
        if (resourceAllowedMethods.nonEmpty) {
          val resourceConfigMethods = new Methods
          resourceConfigMethods.getMethod.addAll(resourceAllowedMethods.asJava)
          configResource.setAllowedMethods(resourceConfigMethods)
        }

        configResource
      }.asJava)
      config.setResources(configResources)
    }

    config
  }

  def allowAllOriginsAndMethods(): Unit = {
    corsFilter.configurationUpdated(createCorsConfig(List(".*"), HttpMethods, List()))
  }

  def setConfiguredAllowedOriginsTo(origins: List[Origin]): Unit = {
    val config = new CorsConfig
    val configOrigins = new Origins
    configOrigins.getOrigin.addAll(origins.asJava)
    config.setAllowedOrigins(configOrigins)

    // allow all of the methods since we're not testing that here
    val configMethods = new Methods
    configMethods.getMethod.addAll(HttpMethods.asJava)
    config.setAllowedMethods(configMethods)

    corsFilter.configurationUpdated(config)
  }
}

object CorsFilterTest {
  implicit def autoRequestWrapper(request: MockHttpServletRequest): HttpServletRequestWrapper =
    new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
}
