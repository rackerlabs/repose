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
package org.openrepose.filters.urinormalization

import java.net.URL
import java.util.Optional
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.HttpHeaders

import com.codahale.metrics.{Meter, MetricRegistry}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, same}
import org.mockito.Mockito.{reset, verify, when}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.{MetricsService, AggregateMeterFactory}
import org.openrepose.filters.urinormalization.config._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class UriNormalizationFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var metricsService: MetricsService = _
  var metricsServiceOpt: Optional[MetricsService] = _
  var summingMeterFactory: AggregateMeterFactory = _
  var meter: Meter = _
  var configurationService: ConfigurationService = _
  var filter: UriNormalizationFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _

  override def beforeEach(): Unit = {
    metricsService = mock[MetricsService]
    summingMeterFactory = mock[AggregateMeterFactory]
    meter = mock[Meter]
    configurationService = mock[ConfigurationService]

    metricsServiceOpt = Optional.of(metricsService)

    when(metricsService.createSummingMeterFactory(anyString())).thenReturn(summingMeterFactory)
    when(summingMeterFactory.createMeter(anyString())).thenReturn(meter)

    servletRequest = new MockHttpServletRequest("GET", "/a/really/nifty/uri")
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain

    servletRequest.setQueryString("a=1&b=2&c=3&d=4")
    servletRequest.setParameter("a", "1")
    servletRequest.setParameter("b", "2")
    servletRequest.setParameter("c", "3")
    servletRequest.setParameter("d", "4")

    filter = new UriNormalizationFilter(configurationService, metricsServiceOpt)
  }

  describe("init") {
    it("should register configuration listener") {
      filter.init(new MockFilterConfig())

      verify(configurationService).subscribeTo(
        anyString(),
        anyString(),
        any[URL],
        same(filter),
        any[Class[UriNormalizationConfig]]
      )
    }
  }

  describe("destroy") {
    it("should unregister configuration listener") {
      filter.destroy()

      verify(configurationService).unsubscribeFrom(anyString(), same(filter))
    }
  }

  describe("isInitialized") {
    it("should return false if the filter has not read a configuration") {
      filter.isInitialized shouldBe false
    }

    it("should return true if the filter has read a configuration") {
      filter.configurationUpdated(new UriNormalizationConfig())

      filter.isInitialized shouldBe true
    }
  }

  describe("doFilter") {
    it("should throw a 503 if the filter has not yet initialized") {
      filter.doFilter(null, servletResponse, null)

      servletResponse.getStatus shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
    }

    it("should filter parameters") {
      filter.configurationUpdated(createConfig(uriFilterTargets = Iterable({
        val t = new Target
        val hupl = new HttpUriParameterList
        val up = new UriParameter
        up.setName("a")
        hupl.getParameter.add(up)
        t.setWhitelist(hupl)
        t.getHttpMethods.add(HttpMethod.GET)
        t
      })))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]

      wrappedRequest.getQueryString shouldBe "a=1"
    }

    it("should not filter on incorrect method") {
      filter.configurationUpdated(createConfig(uriFilterTargets = Iterable({
        val t = new Target
        val hupl = new HttpUriParameterList
        val up = new UriParameter
        up.setName("a")
        hupl.getParameter.add(up)
        t.setWhitelist(hupl)
        t.getHttpMethods.add(HttpMethod.POST)
        t
      })))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]

      wrappedRequest.getQueryString shouldBe "a=1&b=2&c=3&d=4"
    }

    it("should normalize content media type") {
      filter.configurationUpdated(createConfig(mediaTypeVariants = Iterable({
        val mt = new MediaType
        mt.setName("application/xml")
        mt.setPreferred(false)
        mt.setVariantExtension("xml")
        mt
      })))

      val noExtensionUri = servletRequest.getRequestURI
      servletRequest.setRequestURI(noExtensionUri + ".xml")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]

      wrappedRequest.getRequestURI shouldBe noExtensionUri
      wrappedRequest.getHeader(HttpHeaders.ACCEPT) shouldBe "application/xml"
    }

    it("should normalize preferred content media type") {
      filter.configurationUpdated(createConfig(mediaTypeVariants = Iterable({
        val mt = new MediaType
        mt.setName("application/xml")
        mt.setPreferred(true)
        mt.setVariantExtension("xml")
        mt
      })))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val wrappedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]

      wrappedRequest.getHeader(HttpHeaders.ACCEPT) shouldBe "application/xml"
    }
  }

  describe("config") {
    // this consistently triggers the undependable ordering of scala.collection.mutable.HashMap, so test it
    it("should load it in order resulting in the ALL target being used") {
      filter.configurationUpdated(createConfig(uriFilterTargets = Iterable(
        (new Target).withHttpMethods(List(HttpMethod.ALL))
          .withWhitelist((new HttpUriParameterList).withParameter((new UriParameter).withName("a"))),
        (new Target).withHttpMethods(List(HttpMethod.GET))
          .withWhitelist((new HttpUriParameterList).withParameter((new UriParameter).withName("b")))
      )))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getQueryString shouldBe "a=1"
    }
  }

  def createConfig(mediaTypeVariants: Iterable[MediaType] = Iterable.empty, uriFilterTargets: Iterable[Target] = Iterable.empty): UriNormalizationConfig = {
    val config = new UriNormalizationConfig

    config.setMediaVariants(new MediaTypeList())
    mediaTypeVariants.foreach(config.getMediaVariants.getMediaType.add)

    config.setUriFilters(new UriFilterList())
    uriFilterTargets.foreach(config.getUriFilters.getTarget.add)

    config
  }
}
