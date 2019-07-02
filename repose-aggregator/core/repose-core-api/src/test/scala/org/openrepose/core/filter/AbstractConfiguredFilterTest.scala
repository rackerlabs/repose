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
package org.openrepose.core.filter

import java.net.URL

import javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, FilterConfig}
import org.hamcrest.Matchers.{endsWith, hasProperty}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, argThat, same, eq => eql}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletResponse

/**
  * Created by adrian on 4/29/16.
  */
@RunWith(classOf[JUnitRunner])
class AbstractConfiguredFilterTest
  extends FunSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {

  var filter: StubbedFilter = _
  var configurationService: ConfigurationService = _

  before {
    configurationService= mock[ConfigurationService]
    filter = new StubbedFilter(configurationService)
  }

  describe("init method") {
    it("should register with the correct name") {
      val filterConfig = mock[FilterConfig]
      when(filterConfig.getFilterName).thenReturn("banana-filter")

      filter.init(filterConfig)

      verify(configurationService).subscribeTo(eql("banana-filter"), anyString(), any(classOf[URL]),
        any(classOf[UpdateListener[String]]), any(classOf[Class[String]]))
    }

    it("should register with default config name") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), eql("stubbed.cfg.xml"), any(classOf[URL]),
        any(classOf[UpdateListener[String]]), any(classOf[Class[String]]))
    }

    it("should register with a configured config name") {
      val filterConfig = mock[FilterConfig]
      when(filterConfig.getInitParameter("filter-config")).thenReturn("banana.cfg.xml")

      filter.init(filterConfig)

      verify(configurationService).subscribeTo(anyString(), eql("banana.cfg.xml"), any(classOf[URL]),
        any(classOf[UpdateListener[String]]), any(classOf[Class[String]]))
    }

    it("should register with the correct schema location") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), anyString(), argThat(hasProperty("path", endsWith("/stubbed.xsd"))),
        any(classOf[UpdateListener[String]]), any(classOf[Class[String]]))
    }

    it("should register with the correct listener") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), anyString(), any(classOf[URL]),
        same(filter), any(classOf[Class[String]]))
    }

    it("should register with the correct config class") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), anyString(), any(classOf[URL]),
        any(classOf[UpdateListener[String]]), any(classOf[Class[String]]))
    }

    it("should call doInit") {
      var doInitCalled = false

      val aFilter = new StubbedFilter(configurationService) {
        override def doInit(filterConfig: FilterConfig): Unit = doInitCalled = true
      }

      aFilter.init(mock[FilterConfig])

      doInitCalled shouldBe true
    }
  }

  describe("destroy method") {
    it("should unsubscribe with the default config file") {
      filter.init(mock[FilterConfig])

      filter.destroy()

      verify(configurationService).unsubscribeFrom(eql("stubbed.cfg.xml"), any(classOf[UpdateListener[String]]))
    }

    it("should unsubscribe with the configured config file") {
      val filterConfig = mock[FilterConfig]
      when(filterConfig.getInitParameter("filter-config")).thenReturn("banana.cfg.xml")
      filter.init(filterConfig)

      filter.destroy()

      verify(configurationService).unsubscribeFrom(eql("banana.cfg.xml"), any(classOf[UpdateListener[String]]))
    }

    it("should unsubscribe with the correct listener") {

      filter.destroy()

      verify(configurationService).unsubscribeFrom(anyString(), same(filter))
    }

    it("should call doDestroy") {
      var doDestroyCalled = false

      val aFilter = new StubbedFilter(configurationService) {
        override def doDestroy(): Unit = doDestroyCalled = true
      }

      aFilter.destroy()

      doDestroyCalled shouldBe true
    }
  }

  describe("configurationUpdated method") {
    it("should save the configuration") {
      val testConfig: String = "foo"
      filter.configurationUpdated(testConfig)

      filter.getConfig should be theSameInstanceAs testConfig
    }

    it("should set initialized") {
      filter.configurationUpdated("bar")

      filter.getInitialized shouldBe true
    }

    it("should call doConfigurationUpdated") {
      val aFilter = new StubbedFilter(configurationService) {
        override def doConfigurationUpdated(newConfiguration: String): String = "banana"
      }

      aFilter.configurationUpdated("bar")

      aFilter.configuration shouldBe "banana"
    }

    it("should call doConfigurationUpdated before setting the initialization flag") {
      var doConfigurationUpdatedCalled = false

      val aFilter = new StubbedFilter(configurationService) {
        override def doConfigurationUpdated(newConfiguration: String): String = {
          doConfigurationUpdatedCalled = true
          throw new Exception()
        }
      }

      an[Exception] should be thrownBy aFilter.configurationUpdated("bar")
      doConfigurationUpdatedCalled shouldBe true
      aFilter.isInitialized shouldBe false
    }
  }

  describe("isInitialized method") {
    it("should return false before initial config update") {
      filter.isInitialized shouldBe false
    }

    it("should return true after config update") {
      filter.configurationUpdated("baz")

      filter.isInitialized shouldBe true
    }
  }

  describe("filterInitialized method") {
    it("should return false before initial config update") {
      filter.filterInitialized shouldBe false
    }

    it("should return true after config update") {
      filter.configurationUpdated("baz")

      filter.filterInitialized shouldBe true
    }
  }

  describe("doFilter method") {
    it("should 503 if listener is uninitialized") {
      val response: MockHttpServletResponse = new MockHttpServletResponse

      filter.doFilter(mock[HttpServletRequest], response, mock[FilterChain])

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      response.getErrorMessage shouldBe "Filter not initialized"
    }

    it("should 503 if filter says it's not initialized") {
      val aFilter = new StubbedFilter(configurationService) {
        override def filterInitialized = false
      }

      aFilter.configurationUpdated("foo")

      val response: MockHttpServletResponse = new MockHttpServletResponse

      aFilter.doFilter(mock[HttpServletRequest], response, mock[FilterChain])

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      response.getErrorMessage shouldBe "Filter not initialized"
    }

    it("should call doWork when initialized") {
      val request: HttpServletRequest = mock[HttpServletRequest]
      val response: HttpServletResponse = mock[HttpServletResponse]
      val chain: FilterChain = mock[FilterChain]
      filter.configurationUpdated("banana")

      filter.doFilter(request, response, chain)

      filter.passedObjects.request should be theSameInstanceAs request
      filter.passedObjects.response should be theSameInstanceAs response
      filter.passedObjects.chain should be theSameInstanceAs chain
    }
  }
}

class StubbedFilter(configurationService: ConfigurationService) extends AbstractConfiguredFilter[String](configurationService) {

  override val DEFAULT_CONFIG: String = "stubbed.cfg.xml"
  override val SCHEMA_LOCATION: String = "/stubbed.xsd"

  var passedObjects: PassedObjects = _

  def getConfig: String = configuration

  def getInitialized: Boolean = listenerInitialized

  override def doWork(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain): Unit =
    passedObjects = PassedObjects(httpRequest, httpResponse, chain)

  case class PassedObjects(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain)
}
