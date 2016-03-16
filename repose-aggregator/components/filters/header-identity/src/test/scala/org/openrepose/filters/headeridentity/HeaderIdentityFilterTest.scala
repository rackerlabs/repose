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

package org.openrepose.filters.headeridentity

import javax.servlet.ServletResponse

import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.headeridentity.config.{HttpHeaderList, HttpHeader, HeaderIdentityConfig}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, BeforeAndAfter, FunSpec}
import org.scalatest.junit.JUnitRunner
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest}

import collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class HeaderIdentityFilterTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  import HeaderIdentityFilterTest._

  var filter: HeaderIdentityFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: ServletResponse = _
  var filterChain: MockFilterChain = _

  before {
    servletRequest = new MockHttpServletRequest
    servletResponse = mock[ServletResponse]
    filterChain = new MockFilterChain

    filter = new HeaderIdentityFilter(null)
  }

  describe("adding the configured headers") {
    it("adds the configured header if it exists in the request") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("some-header", None))))
      servletRequest.addHeader("some-header", "some-value")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeader(User) shouldBe "some-value" + DefaultQualty
      wrappedRequest.getHeader(Groups) shouldBe "some-header" + DefaultQualty
    }

    it("adds the only configured header that exists in the request when it's configured first in the list") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("some-header", None), ConfigHeader("nope", None))))
      servletRequest.addHeader("some-header", "some-value")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeadersScala(User) should contain theSameElementsAs List("some-value" + DefaultQualty)
      wrappedRequest.getHeadersScala(Groups) should contain theSameElementsAs List("some-header" + DefaultQualty)
    }

    it("adds the only configured header that exists in the request when it's configured second in the list") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("nope", None), ConfigHeader("some-header", None))))
      servletRequest.addHeader("some-header", "some-value")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeadersScala(User) should contain theSameElementsAs List("some-value" + DefaultQualty)
      wrappedRequest.getHeadersScala(Groups) should contain theSameElementsAs List("some-header" + DefaultQualty)
    }

    it("adds both configured headers when they both exist in the request") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("berry", None), ConfigHeader("fruit", None))))
      servletRequest.addHeader("berry", "banana")
      servletRequest.addHeader("fruit", "lemon")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeadersScala(User) should contain theSameElementsAs List("banana" + DefaultQualty, "lemon" + DefaultQualty)
      wrappedRequest.getHeadersScala(Groups) should contain theSameElementsAs List("berry" + DefaultQualty, "fruit" + DefaultQualty)
    }

    it("does NOT add a configured header if it does not exist in the request") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("nope", None))))
      servletRequest.addHeader("best", "north")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeadersScala(User) shouldBe empty
      wrappedRequest.getHeadersScala(Groups) shouldBe empty
    }

    it("does NOT add a configured header when its value in the request is empty") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("maybe", None))))
      servletRequest.addHeader("maybe", "")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeadersScala(User) shouldBe empty
      wrappedRequest.getHeadersScala(Groups) shouldBe empty
    }
  }

  describe("extracting the header values") {
    it("adds the header value using the default quality when one is not configured") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("some-header", None))))
      servletRequest.addHeader("some-header", "some-value")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeader(User) shouldBe "some-value" + DefaultQualty
    }

    it("adds the first header value only") {
      filter.configurationUpdated(createConfig(List(ConfigHeader("some-header", None))))
      servletRequest.addHeader("some-header", "some-value,another-value,yet-another-value,omg,bbq")
      servletRequest.addHeader("some-header", "why-not-one-more")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = getPostFilterRequest
      wrappedRequest.getHeader(User) shouldBe "some-value" + DefaultQualty
    }
  }

  private def getPostFilterRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]

  private def createConfig(configHeaders: List[ConfigHeader]): HeaderIdentityConfig = {
    val config = new HeaderIdentityConfig
    val sourceHeaders = new HttpHeaderList
    config.setSourceHeaders(sourceHeaders)
    sourceHeaders.getHeader.addAll(configHeaders.map(createHttpHeader))
    config
  }

  private def createHttpHeader(configHeader: ConfigHeader): HttpHeader = {
    val httpHeader = new HttpHeader
    httpHeader.setId(configHeader.name)
    configHeader.quality.foreach(httpHeader.setQuality(_))
    httpHeader
  }
}

object HeaderIdentityFilterTest {
  private final val User = PowerApiHeader.USER.toString
  private final val Groups = PowerApiHeader.GROUPS.toString
  private final val DefaultQualty = ";q=0.1"

  case class ConfigHeader(name: String, quality: Option[Double])
}
