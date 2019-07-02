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
package org.openrepose.filters.splitheader

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.junit.runner.RunWith
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.splitheader.config.{HeaderList, SplitHeaderConfig}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SplitHeaderFilterTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  final val TestHeaderName: String = "test"

  var configurationService: ConfigurationService = _
  var splitHeaderFilter: SplitHeaderFilter = _

  var request: MockHttpServletRequest = _
  var response: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    configurationService = mock[ConfigurationService]
    splitHeaderFilter = new SplitHeaderFilter(configurationService)

    request = new MockHttpServletRequest()
    response = new MockHttpServletResponse()

    // A mock filter chain with a Servlet that copies headers from the shared response object
    // into the response object passed to the Servlet.
    // This is necessary because the response wrapper is used in the filter, but does not copy
    // in any headers when it wraps a response.
    // Unfortunately, this couples testing of the Split Header filter with testing of the response
    // wrapper to some degree.
    // This could be avoided by either copying headers in the response wrapper, or not using the
    // response wrapped in the Split Header filter.
    filterChain = new MockFilterChain(new Servlet() {
      override def init(config: ServletConfig): Unit = ???

      override def getServletConfig: ServletConfig = ???

      override def service(req: ServletRequest, res: ServletResponse): Unit = {
        response.getHeaderNames.asScala foreach { headerName =>
          response.getHeaders(headerName).asScala foreach { headerValue =>
            res.asInstanceOf[HttpServletResponse].addHeader(headerName, headerValue)
          }
        }
      }

      override def getServletInfo: String = ???

      override def destroy(): Unit = ???
    })
  }

  describe("doWork") {
    it("should not modify headers if no headers are configured to be split") {
      splitHeaderFilter.configuration = new SplitHeaderConfig()

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaderNames.asScala shouldBe empty
      response.getHeaderNames.asScala shouldBe empty
    }

    it("should not modify headers if no headers match configured headers") {
      val headerName = "Not-Configured"
      val headerValue = "one,two,three"

      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(headerName, headerValue)

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaderNames.asScala.toSet should contain only headerName
      passedRequest.getHeaders(headerName).asScala.toSeq should contain only headerValue
    }

    it("should split a request header with multiple values on the one line") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(TestHeaderName, "one,two,three")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three")
    }

    it("should split a response header with multiple values on the one line") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq(TestHeaderName))

      response.addHeader(TestHeaderName, "one,two,three")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three")
    }

    it("should split a request header with multiple lines each with one value") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(TestHeaderName, "one")
      request.addHeader(TestHeaderName, "two")
      request.addHeader(TestHeaderName, "three")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three")
    }

    it("should split a response header with multiple lines each with one value") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq(TestHeaderName))

      response.addHeader(TestHeaderName, "one")
      response.addHeader(TestHeaderName, "two")
      response.addHeader(TestHeaderName, "three")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three")
    }

    it("should split a request header with multiple values on multiple lines") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(TestHeaderName, "one,two")
      request.addHeader(TestHeaderName, "three,four")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three", "four")
    }

    it("should split a response header with multiple values on multiple lines") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq(TestHeaderName))

      response.addHeader(TestHeaderName, "one,two")
      response.addHeader(TestHeaderName, "two,four")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "four")
    }

    it("should split a request header regardless of casing") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq("test-header"))

      request.addHeader("TEST-HeAdEr", "one,two")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders("test-header").asScala.toSeq should contain only("one", "two")
    }

    it("should split a response header regardless of casing") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq("test-header"))

      response.addHeader("TEST-HeAdEr", "one,two")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders("test-header").asScala.toSeq should contain only("one", "two")
    }

    it("should not change the order of request header values when splitting") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(TestHeaderName, "one,two")
      request.addHeader(TestHeaderName, "three")
      request.addHeader(TestHeaderName, "four")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders(TestHeaderName).asScala.toSeq should contain inOrderOnly("one", "two", "three", "four")
    }

    it("should not change the order of response header values when splitting") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq(TestHeaderName))

      response.addHeader(TestHeaderName, "one,two")
      response.addHeader(TestHeaderName, "three")
      response.addHeader(TestHeaderName, "four")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders(TestHeaderName).asScala.toSeq should contain inOrderOnly("one", "two", "three", "four")
    }

    it("should trim the values of a request header when splitting") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(TestHeaderName, "one, two")
      request.addHeader(TestHeaderName, "three  ")
      request.addHeader(TestHeaderName, "  four")
      request.addHeader(TestHeaderName, "fi  ve")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three", "four", "fi  ve")
    }

    it("should trim the values of a response header when splitting") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq(TestHeaderName))

      response.addHeader(TestHeaderName, "one, two")
      response.addHeader(TestHeaderName, "three")
      response.addHeader(TestHeaderName, "four")
      response.addHeader(TestHeaderName, "fi  ve")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders(TestHeaderName).asScala.toSeq should contain only("one", "two", "three", "four", "fi  ve")
    }

    it("should retain parameters with their associated value on request headers") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq(TestHeaderName))

      request.addHeader(TestHeaderName, "one;q=0.1;foo=bar,two;baz=zaz;q=0.2")
      request.addHeader(TestHeaderName, "three;q=0.3;too=too")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaders(TestHeaderName).asScala.toSeq should contain only("one;q=0.1;foo=bar", "two;baz=zaz;q=0.2", "three;q=0.3;too=too")
    }

    it("should retain parameters with their associated value on response headers") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq(TestHeaderName))

      response.addHeader(TestHeaderName, "one;q=0.1;foo=bar,two;baz=zaz;q=0.2")
      response.addHeader(TestHeaderName, "three;q=0.3;too=too")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaders(TestHeaderName).asScala.toSeq should contain only("one;q=0.1;foo=bar", "two;baz=zaz;q=0.2", "three;q=0.3;too=too")
    }

    it("should adopt the configured request header name casing") {
      splitHeaderFilter.configuration = createConfig(requestHeaders = Seq("TEST"))

      request.addHeader("test", "one,two")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeaderNames.asScala.toSeq should contain("TEST")
    }

    it("should adopt the configured response header name casing") {
      splitHeaderFilter.configuration = createConfig(responseHeaders = Seq("TEST"))

      response.addHeader("test", "one,two")

      splitHeaderFilter.doWork(request, response, filterChain)

      val passedResponse = filterChain.getResponse.asInstanceOf[HttpServletResponse]
      passedResponse.getHeaderNames.asScala.toSeq should contain("TEST")
    }
  }

  def createConfig(requestHeaders: Seq[String] = Seq.empty, responseHeaders: Seq[String] = Seq.empty): SplitHeaderConfig = {
    val config = new SplitHeaderConfig()

    if (requestHeaders.nonEmpty) {
      val requestHeaderList = new HeaderList()
      requestHeaderList.getHeader.addAll(requestHeaders.asJavaCollection)
      config.setRequest(requestHeaderList)
    }

    if (responseHeaders.nonEmpty) {
      val responseHeaderList = new HeaderList()
      responseHeaderList.getHeader.addAll(responseHeaders.asJavaCollection)
      config.setResponse(responseHeaderList)
    }

    config
  }
}
