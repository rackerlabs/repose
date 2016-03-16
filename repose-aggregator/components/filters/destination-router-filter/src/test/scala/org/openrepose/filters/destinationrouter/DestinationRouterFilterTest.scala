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
package org.openrepose.filters.destinationrouter

import java.net.URL

import com.mockrunner.mock.web.MockHttpServletResponse
import org.mockito.Matchers.{any, anyString, same}
import org.mockito.Mockito.{reset, verify}
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.commons.utils.servlet.http.RouteDestination
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.filters.routing.servlet.config.{DestinationRouterConfiguration, Target}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest}

class DestinationRouterFilterTest extends FunSpec with Matchers with BeforeAndAfter with MockitoSugar {

  val configurationService = mock[ConfigurationService]
  val metricsService = mock[MetricsService]

  before {
    reset(configurationService)
    reset(metricsService)
  }

  describe("init") {
    it("should register with the configuration service") {
      val filter = new DestinationRouterFilter(configurationService, metricsService)

      filter.init(new MockFilterConfig())

      verify(configurationService).subscribeTo(anyString(), anyString(), any[URL], same(filter), any[Class[DestinationRouterConfiguration]])
    }
  }

  describe("destroy") {
    it("should un-register with the configuration service") {
      val filter = new DestinationRouterFilter(configurationService, metricsService)

      filter.destroy()

      verify(configurationService).unsubscribeFrom(anyString(), same(filter))
    }
  }

  describe("doFilter") {
    it("should do nothing if the id is blank") {
      val request = new MockHttpServletRequest()
      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()
      val filter = new DestinationRouterFilter(configurationService, metricsService)

      val config = new DestinationRouterConfiguration()
      val target = new Target()
      target.setId("")
      config.setTarget(target)

      filter.configurationUpdated(config)
      filter.doFilter(request, response, chain)

      request.getAttribute(CommonRequestAttributes.DESTINATIONS) shouldBe null
    }

    it("should add a destination id to the request with a default quality of 0.5") {
      val request = new MockHttpServletRequest()
      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()
      val filter = new DestinationRouterFilter(configurationService, metricsService)

      val config = new DestinationRouterConfiguration()
      val target = new Target()
      target.setId("foo")
      config.setTarget(target)

      filter.configurationUpdated(config)
      filter.doFilter(request, response, chain)

      request.getAttribute(CommonRequestAttributes.DESTINATIONS).asInstanceOf[java.util.List[RouteDestination]] should have size 1
      request.getAttribute(CommonRequestAttributes.DESTINATIONS).asInstanceOf[java.util.List[RouteDestination]].get(0).getDestinationId shouldBe "foo"
      request.getAttribute(CommonRequestAttributes.DESTINATIONS).asInstanceOf[java.util.List[RouteDestination]].get(0).getQuality shouldBe 0.5
    }

    it("should add a destination id to the request with a custom quality") {
      val request = new MockHttpServletRequest()
      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()
      val filter = new DestinationRouterFilter(configurationService, metricsService)

      val config = new DestinationRouterConfiguration()
      val target = new Target()
      target.setId("foo")
      target.setQuality(0.7)
      config.setTarget(target)

      filter.configurationUpdated(config)
      filter.doFilter(request, response, chain)

      request.getAttribute(CommonRequestAttributes.DESTINATIONS).asInstanceOf[java.util.List[RouteDestination]] should have size 1
      request.getAttribute(CommonRequestAttributes.DESTINATIONS).asInstanceOf[java.util.List[RouteDestination]].get(0).getDestinationId shouldBe "foo"
      request.getAttribute(CommonRequestAttributes.DESTINATIONS).asInstanceOf[java.util.List[RouteDestination]].get(0).getQuality shouldBe 0.7
    }
  }
}
