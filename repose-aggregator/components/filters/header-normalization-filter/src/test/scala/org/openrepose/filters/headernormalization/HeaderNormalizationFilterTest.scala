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

import java.util.Optional
import javax.servlet.http.HttpServletResponse
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse}

import com.codahale.metrics.Meter
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.{MetricsService, AggregateMeterFactory}
import org.openrepose.filters.headernormalization.config._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HeaderNormalizationFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import HeaderNormalizationFilterTest._

  final val METRIC_PREFIX = "org.openrepose.filters.headernormalization.HeaderNormalizationFilter.Normalization"

  var filter: HeaderNormalizationFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  var filterConfig: FilterConfig = _
  var metricsService: MetricsService = _
  var summingMeterFactory: AggregateMeterFactory = _
  var meter: Meter = _

  override def beforeEach(): Unit = {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse

    filterChain = mock[FilterChain]
    filterConfig = mock[FilterConfig]
    metricsService = mock[MetricsService]
    summingMeterFactory = mock[AggregateMeterFactory]
    meter = mock[Meter]

    when(filterConfig.getInitParameterNames).thenReturn(List.empty[String].toIterator.asJavaEnumeration)
    when(metricsService.createSummingMeterFactory(anyString())).thenReturn(summingMeterFactory)
    when(summingMeterFactory.createMeter(anyString())).thenReturn(meter)

    filter = new HeaderNormalizationFilter(mock[ConfigurationService], Optional.of(metricsService))
    filter.init(filterConfig)
  }

  // TODO for v10.0.0.0: Only the New Style config should be tested.
  Seq((RequestTarget, false), (RequestTarget, true), (ResponseTarget, true)) foreach { case (target, newStyle) =>
    //Seq((RequestTarget.asInstanceOf[TargetType], false)) foreach { case (target, newStyle) =>
    describe("white list") {
      it(s"will keep white listed headers and remove non-white listed headers on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("cool-header", "so-contents"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header", "cool-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("cool-header", "so-contents"))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will remove non-white listed headers on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("cool-header", "so-contents"), ("should-not-see-me", "wow"), ("me-neither", "wut"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header", "cool-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("should-not-see-me", null), ("me-neither", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will NOT error out on configured headers that don't exist in the request on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("exists-header", "potato"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("exists-header", "does-not-exist-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("exists-header", "potato"), ("does-not-exist-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will NOT remove a white listed header with different casing on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-HEADER", "such-value"), ("do-you-want", "fries"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("do-you-want", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }
    }

    describe("black list") {
      it(s"will remove black listed headers on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("dangerous-header", "all-the-permissions"), ("too-hip-for-you-header", "42"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("dangerous-header", "too-hip-for-you-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("dangerous-header", null), ("too-hip-for-you-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will keep non-black listed headers on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("dangerous-header", "all-the-permissions"), ("not-so-bad-header", "lettuce"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("dangerous-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("not-so-bad-header", "lettuce"))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will not error out on configured headers that don't exist in the request on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("not-so-bad-header", "lettuce"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("dangerous-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("not-so-bad-header", "lettuce"))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will remove a black listed header with different casing on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("DANGERous-header", "all-the-permissions"), ("TOO-hip-for-YOU-header", "42"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("dangerous-header", "too-hip-for-you-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("dangerous-header", null), ("DANGEROUS-header", null), ("too-hip-for-you-header", null), ("TOO-hip-for-YOU-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }
    }

    describe("url/method matching") {
      it(s"will use the target matching a configured URL on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("nope-header", "nope nope"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/ham")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("nope-header"), Some("/ham"), None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("nope-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching a configured URL regex on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/v1/servers/1423729/status")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), Some("/v1/servers/[^/]+/status"), None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching without a configured URL regex on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/ham")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will match all applicable targets for configured URLs on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"), ("terrible-header", "foo"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/ham")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), Some("/.*"), None),
                                                 ConfigTarget(target, BlackList, List("terrible-header"), Some("/ham"), None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null), ("terrible-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching a single configured method on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("PATCH")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), None, Some(List("PATCH")))))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching one of a list of configured methods on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("POST")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), None, Some(List("PATCH", "POST", "GET", "DELETE")))))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching a configured method of ALL on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("GET")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), None, Some(List("ALL")))))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching without a configured method on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("bad-header", "meh"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("DELETE")
        val config = createConfig(newStyle, List(ConfigTarget(target, BlackList, List("bad-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("bad-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching a configured URL and method after not matching a non-matching target on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("nope-header", "nope nope"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("DELETE")
        servletRequest.setRequestURI("/v2/servers/51938")
        val config = createConfig(newStyle, List(
          ConfigTarget(target, BlackList, List("legit-header"), Some("/v1/.*"), Some(List("GET", "POST"))),
          ConfigTarget(target, BlackList, List("nope-header"), Some("/v2/.*"), Some(List("PATCH", "DELETE")))))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("nope-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }

      it(s"will use the target matching without a URL and method after not matching a non-match target on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"), ("nope-header", "nope nope"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("PATCH")
        servletRequest.setRequestURI("/fries")
        val config = createConfig(newStyle, List(
          ConfigTarget(target, BlackList, List("legit-header"), Some("/v1/.*"), Some(List("GET", "POST"))),
          ConfigTarget(target, BlackList, List("nope-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        val postHeaders = Seq(("legit-header", "such-value"), ("nope-header", null))
        target match {
          case RequestTarget => requestHeadersShouldBe(postHeaders)
          case ResponseTarget => responseHeadersShouldBe(postHeaders)
        }
      }
    }

    describe("metrics") {
      it(s"will update metrics when a request matches a config target when using the default catch-all URL on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setMethod("PATCH")
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header"), None, None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        verify(metricsService).createSummingMeterFactory(s"$METRIC_PREFIX.${requestResponseTypeToString(target)}")
        verify(summingMeterFactory).createMeter(s"PATCH._*")
      }

      it(s"will update metrics when a request matches a config target when using a specified URL on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/v1/servers/web-server-01/status")
        servletRequest.setMethod("GET")
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header"), Some("/v1/servers/[^/]+/status"), None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        verify(metricsService).createSummingMeterFactory(s"$METRIC_PREFIX.${requestResponseTypeToString(target)}")
        verify(summingMeterFactory).createMeter(s"GET./v1/servers/[^/]+/status")
      }

      it(s"will NOT update metrics when a request does not match any config target on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/v2/fries")
        servletRequest.setMethod("GET")
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header"), Some("/v1/servers/[^/]+/status"), None)))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        verify(meter, never()).mark(any())
      }

      it(s"will NOT update metrics when a request method does not match any config target on the ${requestResponseTypeWithStyleToString(target, newStyle)}") {
        val preHeaders = Seq(("legit-header", "such-value"))
        target match {
          case RequestTarget => addRequestHeaders(preHeaders)
          case ResponseTarget => addResponseHeadersOnDoFilter(preHeaders)
        }
        servletRequest.setRequestURI("/v1/servers/web-server-01/status")
        servletRequest.setMethod("GET")
        val config = createConfig(newStyle, List(ConfigTarget(target, WhiteList, List("legit-header"), Some("/v1/servers/[^/]+/status"), Some(List("POST")))))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        verify(meter, never()).mark(any())
      }
    }
  }

  def createConfig(newStyle: Boolean, configuredTargets: List[ConfigTarget]): HeaderNormalizationConfig = {
    if (newStyle) createConfig(configuredTargets)
    else createOldConfig(configuredTargets)
  }

  def createConfig(configuredTargets: List[ConfigTarget]): HeaderNormalizationConfig = {
    val config = new HeaderNormalizationConfig
    // each "target" in config
    config.getTarget.asScala ++= configuredTargets.map { configTarget =>
      val target = new Target

      // list will either be "blacklist" or "whitelist"
      val httpHeaderList = new HttpHeaderList
      httpHeaderList.getHeader.asScala ++= configTarget.headers.map { headerName =>
        val httpHeader = new HttpHeader
        httpHeader.setId(headerName)
        httpHeader
      }

      // The RequestResponse the "blacklist" or "whitelist" belongs to
      val requestResponse = new RequestResponse
      configTarget.access match {
        case WhiteList => requestResponse.setWhitelist(httpHeaderList)
        case BlackList => requestResponse.setBlacklist(httpHeaderList)
      }

      configTarget.targetType match {
        case RequestTarget => target.setRequest(requestResponse)
        case ResponseTarget => target.setResponse(requestResponse)
      }

      configTarget.uri.foreach(target.setUriRegex)

      configTarget.methods.foreach(target.getHttpMethods.asScala ++= _.map(HttpMethod.fromValue))

      target
    }
    config
  }

  def createOldConfig(configuredTargets: List[ConfigTarget]): HeaderNormalizationConfig = {
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

  def addRequestHeaders(headers: Seq[(String, String)]): Unit = {
    headers foreach { case (hdrName, hdrValue) => servletRequest.addHeader(hdrName, hdrValue) }
  }

  def getPostFilterRequest: HttpServletRequestWrapper = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
    requestCaptor.getValue
  }

  def addResponseHeadersOnDoFilter(headers: Seq[(String, String)]): Unit = {
    val responseCaptor = ArgumentCaptor.forClass(classOf[HttpServletResponse])
    // only add the headers to the response when the filterChain doFilter method is called
    doAnswer(new Answer[Void]() {
      def answer(invocation: InvocationOnMock): Void = {
        headers foreach { case (hdrName, hdrValue) => responseCaptor.getValue.addHeader(hdrName, hdrValue) }
        null
      }
    }).when(filterChain).doFilter(any(classOf[ServletRequest]), responseCaptor.capture())
  }

  def requestHeadersShouldBe(headers: Seq[(String, String)]): Unit = {
    val postFilterRequest = getPostFilterRequest
    headers foreach { case (hdrName, hdrValue) => postFilterRequest.getHeader(hdrName) shouldBe hdrValue }
  }

  def responseHeadersShouldBe(headers: Seq[(String, String)]): Unit = {
    headers foreach { case (hdrName, hdrValue) => servletResponse.getHeader(hdrName) shouldBe hdrValue }
  }
}

object HeaderNormalizationFilterTest {

  sealed trait TargetType

  object RequestTarget extends TargetType

  object ResponseTarget extends TargetType

  sealed trait AccessList

  object WhiteList extends AccessList

  object BlackList extends AccessList

  case class ConfigTarget(targetType: TargetType, access: AccessList, headers: List[String], uri: Option[String], methods: Option[Iterable[String]])

  private def getStyle(newStyle: Boolean): String = {
    if (newStyle) "new style"
    else "old style"
  }

  def requestResponseTypeWithStyleToString(targetType: TargetType, newStyle: Boolean): String = targetType match {
    case RequestTarget => s"${getStyle(newStyle)} request"
    case ResponseTarget => s"${getStyle(newStyle)} response"
  }

  def requestResponseTypeToString(targetType: TargetType): String = targetType match {
    case RequestTarget => s"request"
    case ResponseTarget => s"response"
  }
}
