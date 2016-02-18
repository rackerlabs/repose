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

package org.openrepose.filters.headeridmapping

import javax.servlet.{ServletResponse, FilterChain}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.headeridmapping.config.{HttpHeader, HttpHeaderList, HeaderIdMappingConfig}
import org.scalatest.{Matchers, BeforeAndAfter, FunSpec}
import org.scalatest.junit.JUnitRunner
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConverters._
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class HeaderIdMappingFilterTest extends FunSpec with BeforeAndAfter with Matchers {

  import HeaderIdMappingFilterTest._
  import PowerApiHeader._

  var filter: HeaderIdMappingFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: ServletResponse = _
  var filterChain: FilterChain = _

  before {
    servletRequest = new MockHttpServletRequest
    servletResponse = mock(classOf[ServletResponse])
    filterChain = mock(classOf[FilterChain])

    filter = new HeaderIdMappingFilter(null)
  }

  describe("adding the user header") {
    it("adds the user header if the request contains the configured user-header") {
      servletRequest.addHeader("user-abc", "value123")
      val config = createConfig(List(UserGroupQuality("user-abc", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe "value123" + DefaultQuality
    }

    it("does NOT add the user header if the request does NOT contain any configured user-header") {
      servletRequest.addHeader("user-abc", "value123")
      val config = createConfig(List(UserGroupQuality("jank-abc", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe null
    }

    it("does NOT add the user header if the request contains the configured user-header with an empty value") {
      servletRequest.addHeader("user-abc", "")
      val config = createConfig(List(UserGroupQuality("user-abc", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe null
    }

    it("does NOT add the user header if the request contains the configured user-header with a whitespace value") {
      servletRequest.addHeader("user-abc", "           ")
      val config = createConfig(List(UserGroupQuality("user-abc", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe null
    }

    it("adds the first user header that matches config and not a second match") {
      servletRequest.addHeader("user-abc", "spicy-tortilla")
      servletRequest.addHeader("user-xyz", "salty-pirate")
      val config = createConfig(List(
        UserGroupQuality("user-abc", DoesNotMatter, None), UserGroupQuality("user-xyz", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe "spicy-tortilla" + DefaultQuality
    }

    it("adds the first user header that matches skipping non-matching configured headers") {
      servletRequest.addHeader("user-xyz", "spicy-tortilla")
      servletRequest.addHeader("user-abc", "salty-pirate")
      val config = createConfig(List(
        UserGroupQuality("user-abc", DoesNotMatter, None), UserGroupQuality("user-xyz", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe "salty-pirate" + DefaultQuality
    }

    it("only adds the first header value when it's comma-separated in the request") {
      servletRequest.addHeader("user-abc", "chocolate,vanilla")
      val config = createConfig(List(UserGroupQuality("user-abc", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe "chocolate" + DefaultQuality
    }
  }

  describe("adding the group header") {
    it("adds the group header if the request contains the configured user-header and group-header") {
      servletRequest.addHeader("some-user-header", "some-value")
      servletRequest.addHeader("group-789", "want_fries_with_that")
      val config = createConfig(List(UserGroupQuality("some-user-header", "group-789", None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(GROUPS) shouldBe "want_fries_with_that" + DefaultQuality
    }

    it("does NOT add a configured group-header if the user-header does not exist in the request") {
      servletRequest.addHeader("some-user-header-nope", "some-value")
      servletRequest.addHeader("group-789", "want_fries_with_that")
      val config = createConfig(List(UserGroupQuality("some-user-header", "group-789", None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(GROUPS) shouldBe null
    }

    it("does NOT add a non-matching group-header associated with a user-header that does exist in the request") {
      servletRequest.addHeader("some-user-header", "some-value")
      servletRequest.addHeader("group-789-nope", "want_fries_with_that")
      val config = createConfig(List(UserGroupQuality("some-user-header", "group-789", None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(GROUPS) shouldBe null
    }

    it("does NOT add a configured group-header if the configured header has an empty value") {
      servletRequest.addHeader("some-user-header", "some-value")
      servletRequest.addHeader("group-789", "")
      val config = createConfig(List(UserGroupQuality("some-user-header", "group-789", None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(GROUPS) shouldBe null
    }

    it("does NOT add a configured group-header if the configured header has a whitespace value") {
      servletRequest.addHeader("some-user-header", "some-value")
      servletRequest.addHeader("group-789", "        ")
      val config = createConfig(List(UserGroupQuality("some-user-header", "group-789", None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(GROUPS) shouldBe null
    }

    it("only adds the first header value when it's comma-separated in the request") {
      servletRequest.addHeader("some-user-header", "some-value")
      servletRequest.addHeader("group-789", "birthday_cake_ice_cream,want_fries_with_that")
      val config = createConfig(List(UserGroupQuality("some-user-header", "group-789", None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(GROUPS) shouldBe "birthday_cake_ice_cream" + DefaultQuality
    }
  }

  describe("setting the header quality") {
    it("adds the configured quality to the user-header and group-header") {
      servletRequest.addHeader("user-abc", "value123")
      val config = createConfig(List(UserGroupQuality("user-abc", DoesNotMatter, Some(0.8d))))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe "value123;q=0.8"
    }

    it("adds the default quality to the user-header and group-header when none is configured") {
      servletRequest.addHeader("user-abc", "value123")
      val config = createConfig(List(UserGroupQuality("user-abc", DoesNotMatter, None)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(USER) shouldBe "value123" + DefaultQuality
    }
  }

  def createConfig(configuredHeaders: Iterable[UserGroupQuality]): HeaderIdMappingConfig = {
    val config = new HeaderIdMappingConfig
    val sourceHeaders = new HttpHeaderList
    config.setSourceHeaders(sourceHeaders)

    sourceHeaders.getHeader.asScala ++= configuredHeaders.map { userGroupQuality =>
      val httpHeader = new HttpHeader
      httpHeader.setUserHeader(userGroupQuality.user)
      httpHeader.setGroupHeader(userGroupQuality.group)
      userGroupQuality.quality.foreach(httpHeader.setQuality(_))
      httpHeader
    }

    config
  }
  def getPostFilterRequest: HttpServletRequestWrapper = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
    requestCaptor.getValue
  }
}

object HeaderIdMappingFilterTest {
  val DoesNotMatter = "potato"
  val DefaultQuality = ";q=0.1"

  implicit def autoPowerApiHeaderToString(p: PowerApiHeader): String = p.toString

  case class UserGroupQuality(user: String, group: String, quality: Option[Double])
}
