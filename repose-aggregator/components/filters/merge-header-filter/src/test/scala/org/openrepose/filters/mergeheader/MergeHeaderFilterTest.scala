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
package org.openrepose.filters.mergeheader

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.verify
import org.openrepose.core.services.config.ConfigurationService
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class MergeHeaderFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var configurationService: ConfigurationService = _
  var filter: MergeHeaderFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _

  override def beforeEach() = {
    configurationService = mock[ConfigurationService]

    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain

    filter = new MergeHeaderFilter(configurationService)
  }

  describe("init") {
    it("should register a configuration listener") {
      filter.init(new MockFilterConfig("MergeHeaderFilter"))

      verify(configurationService).subscribeTo(anyString(), anyString(), any(), any(), any[Class[MergeHeaderConfig]])
    }
  }

  describe("destroy") {
    it("should unregister a configuration listener") {
      filter.destroy()

      verify(configurationService).unsubscribeFrom(anyString(), any())
    }
  }

  describe("doFilter") {
    it("should throw a 503 if the filter has not yet initialized") {
      filter.doFilter(null, servletResponse, null)

      servletResponse.getStatus shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
    }

    it("should merge request header values into a single comma-delimited line") {
      filter.configurationUpdated(createConfig(Seq("Accept"), Seq.empty[String]))

      servletRequest.addHeader("Accept", "foo")
      servletRequest.addHeader("Accept", "bar")
      servletRequest.addHeader("Accept", "baz")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val passedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      passedRequest.getHeader("Accept") should (include("foo") and include("bar") and include("baz"))
    }

    it("should merge response header values into a single comma-delimited line") {
      filter.configurationUpdated(createConfig(Seq.empty[String], Seq("Accept")))

      servletResponse.addHeader("Accept", "foo")
      servletResponse.addHeader("Accept", "bar")
      servletResponse.addHeader("Accept", "baz")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getHeader("Accept") should (include("foo") and include("bar") and include("baz"))
    }

    it("should not modify request headers if the configured header is not present") {
      filter.configurationUpdated(createConfig(Seq("Accept"), Seq.empty[String]))

      servletRequest.addHeader("Foo", "bar")
      servletRequest.addHeader("Foo", "baz")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletRequest.getHeaderNames.toSet should contain only "Foo"
      servletRequest.getHeaders("Foo").toSeq should (contain("bar") and contain("baz"))
    }

    it("should not modify response headers if the configured header is not present") {
      filter.configurationUpdated(createConfig(Seq.empty[String], Seq("Accept")))

      servletResponse.addHeader("Foo", "bar")
      servletResponse.addHeader("Foo", "baz")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getHeaderNames.toSet should contain only "Foo"
      servletResponse.getHeaders("Foo").toSeq should (contain("bar") and contain("baz"))
    }
  }

  def createConfig(requestHeaders: Iterable[String], responseHeaders: Iterable[String]): MergeHeaderConfig = {
    val config = new MergeHeaderConfig

    val requestHeaderList = new HeaderList
    requestHeaderList.getHeader.addAll(requestHeaders)

    val responseHeaderList = new HeaderList
    responseHeaderList.getHeader.addAll(responseHeaders)

    config.setRequest(requestHeaderList)
    config.setResponse(responseHeaderList)

    config
  }
}
