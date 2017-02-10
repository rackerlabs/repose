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

package org.openrepose.filters.headernormalization

import javax.servlet.{FilterChain, FilterConfig, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.{MeterByCategorySum, MetricsService}
import org.openrepose.filters.headernormalization.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HeaderNormalizationFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import HeaderNormalizationFilterTest._

  var filter: HeaderNormalizationFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: ServletResponse = _
  var filterChain: FilterChain = _
  var filterConfig: FilterConfig = _
  var metricsService: MetricsService = _
  var metricsMeters: MeterByCategorySum = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = mock[ServletResponse]
    filterChain = mock[FilterChain]
    filterConfig = mock[FilterConfig]
    metricsService = mock[MetricsService]
    metricsMeters = mock[MeterByCategorySum]

    when(filterConfig.getInitParameterNames).thenReturn(List.empty[String].toIterator.asJavaEnumeration)
    when(metricsService.newMeterByCategorySum(any(), any(), any(), any())).thenReturn(metricsMeters)

    filter = new HeaderNormalizationFilter(mock[ConfigurationService], metricsService)
    filter.init(filterConfig)
  }

  describe("white list") {
    it("will keep white listed headers and remove non-white listed headers") {
      val config = createConfig(List(ConfigTarget(List("legit-header", "cool-header"), WhiteList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("cool-header", "so-contents")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("cool-header") shouldBe "so-contents"
    }

    it("will remove non-white listed headers") {
      val config = createConfig(List(ConfigTarget(List("legit-header", "cool-header"), WhiteList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("cool-header", "so-contents")
      servletRequest.addHeader("should-not-see-me", "wow")
      servletRequest.addHeader("me-neither", "wut")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("should-not-see-me") shouldBe null
      postFilterRequest.getHeader("me-neither") shouldBe null
    }

    it("will NOT error out on configured headers that don't exist in the request") {
      val config = createConfig(List(ConfigTarget(List("exists-header", "does-not-exist-header"), WhiteList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("exists-header", "potato")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("exists-header") shouldBe "potato"
      postFilterRequest.getHeader("does-not-exist-header") shouldBe null
    }

    it("will NOT remove a white listed header with different casing") {
      val config = createConfig(List(ConfigTarget(List("legit-header"), WhiteList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-HEADER", "such-value")
      servletRequest.addHeader("do-you-want", "fries")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("do-you-want") shouldBe null
    }
  }

  describe("black list") {
    it("will remove black listed headers") {
      val config = createConfig(List(ConfigTarget(List("dangerous-header", "too-hip-for-you-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("dangerous-header", "all-the-permissions")
      servletRequest.addHeader("too-hip-for-you-header", "42")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("dangerous-header") shouldBe null
      postFilterRequest.getHeader("too-hip-for-you-header") shouldBe null
    }

    it("will keep non-black listed headers") {
      val config = createConfig(List(ConfigTarget(List("dangerous-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("dangerous-header", "all-the-permissions")
      servletRequest.addHeader("not-so-bad-header", "lettuce")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("not-so-bad-header") shouldBe "lettuce"
    }

    it("will not error out on configured headers that don't exist in the request") {
      val config = createConfig(List(ConfigTarget(List("dangerous-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("not-so-bad-header", "lettuce")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("not-so-bad-header") shouldBe "lettuce"
    }

    it("will remove a black listed header with different casing") {
      val config = createConfig(List(ConfigTarget(List("dangerous-header", "too-hip-for-you-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("DANGERous-header", "all-the-permissions")
      servletRequest.addHeader("TOO-hip-for-YOU-header", "42")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("dangerous-header") shouldBe null
      postFilterRequest.getHeader("DANGEROUS-header") shouldBe null
      postFilterRequest.getHeader("too-hip-for-you-header") shouldBe null
      postFilterRequest.getHeader("TOO-hip-for-YOU-header") shouldBe null
    }
  }

  describe("url/method matching") {
    it("will use the target matching a configured URL") {
      val config = createConfig(List(ConfigTarget(List("nope-header"), BlackList, Some("/ham"), None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("nope-header", "nope nope")
      servletRequest.setRequestURI("/ham")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("nope-header") shouldBe null
    }

    it("will use the target matching a configured URL regex") {
      val config = createConfig(List(ConfigTarget(List("bad-header"), BlackList, Some("/v1/servers/[^/]+/status"), None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("bad-header", "meh")
      servletRequest.setRequestURI("/v1/servers/1423729/status")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("bad-header") shouldBe null
    }

    it("will use the target matching without a configured URL regex") {
      val config = createConfig(List(ConfigTarget(List("bad-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("bad-header", "meh")
      servletRequest.setRequestURI("/ham")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("bad-header") shouldBe null
    }

    it("will use the target matching a single configured method") {
      val config = createConfig(List(ConfigTarget(List("bad-header"), BlackList, None, Some(List("PATCH")))))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("bad-header", "meh")
      servletRequest.setMethod("PATCH")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("bad-header") shouldBe null
    }

    it("will use the target matching one of a list of configured methods") {
      val config = createConfig(List(ConfigTarget(List("bad-header"), BlackList, None, Some(List("PATCH", "POST", "GET", "DELETE")))))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("bad-header", "meh")
      servletRequest.setMethod("POST")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("bad-header") shouldBe null
    }

    it("will use the target matching a configured method of ALL") {
      val config = createConfig(List(ConfigTarget(List("bad-header"), BlackList, None, Some(List("ALL")))))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("bad-header", "meh")
      servletRequest.setMethod("GET")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("bad-header") shouldBe null
    }

    it("will use the target matching without a configured method") {
      val config = createConfig(List(ConfigTarget(List("bad-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("bad-header", "meh")
      servletRequest.setMethod("DELETE")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("bad-header") shouldBe null
    }

    it("will use the target matching a configured URL and method after not matching a non-matching target") {
      val config = createConfig(List(
        ConfigTarget(List("legit-header"), BlackList, Some("/v1/.*"), Some(List("GET", "POST"))),
        ConfigTarget(List("nope-header"), BlackList, Some("/v2/.*"), Some(List("PATCH", "DELETE")))))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("nope-header", "nope nope")
      servletRequest.setMethod("DELETE")
      servletRequest.setRequestURI("/v2/servers/51938")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("nope-header") shouldBe null
    }

    it("will use the target matching without a URL and method after not matching a non-match target") {
      val config = createConfig(List(
        ConfigTarget(List("legit-header"), BlackList, Some("/v1/.*"), Some(List("GET", "POST"))),
        ConfigTarget(List("nope-header"), BlackList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.addHeader("nope-header", "nope nope")
      servletRequest.setMethod("PATCH")
      servletRequest.setRequestURI("/fries")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("legit-header") shouldBe "such-value"
      postFilterRequest.getHeader("nope-header") shouldBe null
    }
  }

  describe("metrics") {
    it("will update metrics when a request matches a config target when using the default catch-all URL") {
      val config = createConfig(List(ConfigTarget(List("legit-header"), WhiteList, None, None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.setMethod("PATCH")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(metricsMeters).mark(".*_PATCH")
    }

    it("will update metrics when a request matches a config target when using a specified URL") {
      val config = createConfig(List(ConfigTarget(List("legit-header"), WhiteList, Some("/v1/servers/[^/]+/status"), None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.setRequestURI("/v1/servers/web-server-01/status")
      servletRequest.setMethod("GET")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(metricsMeters).mark("/v1/servers/[^/]+/status_GET")
    }

    it("will NOT update metrics when a request does not match any config target") {
      val config = createConfig(List(ConfigTarget(List("legit-header"), WhiteList, Some("/v1/servers/[^/]+/status"), None)))
      filter.configurationUpdated(config)
      servletRequest.addHeader("legit-header", "such-value")
      servletRequest.setRequestURI("/v2/fries")
      servletRequest.setMethod("GET")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(metricsMeters, never()).mark(any())
    }
  }

  def createConfig(configuredTargets: List[ConfigTarget]): HeaderNormalizationConfig = {
    val config = new HeaderNormalizationConfig
    val headerFilterList = new HeaderFilterList
    config.setHeaderFilters(headerFilterList)

    // each "target" in config
    headerFilterList.getTarget.asScala ++= configuredTargets.map { configTarget =>
      val target = new Target

      // list will either be "blacklist" or "whitelist"
      val httpHeaderList = new HttpHeaderList
      httpHeaderList.getHeader.asScala ++= configTarget.headers.map { headerName =>
        val httpHeader = new HttpHeader
        httpHeader.setId(headerName)
        httpHeader
      }

      configTarget.access match {
        case WhiteList => target.getWhitelist.add(httpHeaderList)
        case BlackList => target.getBlacklist.add(httpHeaderList)
      }

      configTarget.uri.foreach(target.setUriRegex)

      configTarget.methods.foreach(target.getHttpMethods.asScala ++= _.map(HttpMethod.fromValue))

      target
    }

    config
  }

  def getPostFilterRequest: HttpServletRequestWrapper = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
    requestCaptor.getValue
  }
}

object HeaderNormalizationFilterTest {
  sealed trait AccessList
  object WhiteList extends AccessList
  object BlackList extends AccessList

  case class ConfigTarget(headers: List[String], access: AccessList, uri: Option[String], methods: Option[Iterable[String]])
}
