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
package org.openrepose.filters.versioning

import java.net.URL
import java.util.Optional
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.codahale.metrics.{Meter, MetricRegistry}
import org.apache.http.HttpHeaders
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.systemmodel.config._
import org.openrepose.filters.versioning.config.{MediaType, MediaTypeList, ServiceVersionMapping, ServiceVersionMappingList}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class VersioningFilterTest extends FunSpec with Matchers with BeforeAndAfterEach with MockitoSugar {

  val systemModel = new SystemModel()
  val destinationList = new DestinationList()
  val endpoint = new Destination()
  endpoint.setId("endpoint")
  endpoint.setDefault(true)
  destinationList.getEndpoint.add(endpoint)
  systemModel.setDestinations(destinationList)

  var configurationService: ConfigurationService = _
  var metricsService: MetricsService = _
  var metricsServiceOpt: Optional[MetricsService] = _
  var metricRegistry: MetricRegistry = _
  var meter: Meter = _
  var request: MockHttpServletRequest = _
  var response: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _
  var filter: VersioningFilter = _
  var systemModelListener: UpdateListener[SystemModel] = _
  var versioningListener: UpdateListener[ServiceVersionMappingList] = _

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]
    metricsService = mock[MetricsService]
    metricRegistry = mock[MetricRegistry]
    meter = mock[Meter]

    metricsServiceOpt = Optional.of(metricsService)

    when(metricsService.getRegistry).thenReturn(metricRegistry)
    when(metricRegistry.meter(anyString())).thenReturn(meter)

    request = new MockHttpServletRequest()
    response = new MockHttpServletResponse()
    filterChain = new MockFilterChain()

    val systemModelListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[SystemModel]])
    val versioningListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[ServiceVersionMappingList]])
    doNothing().when(configurationService).subscribeTo(anyString(), anyString(), any[URL], versioningListenerCaptor.capture(), any[Class[ServiceVersionMappingList]])
    doNothing().when(configurationService).subscribeTo(anyString(), systemModelListenerCaptor.capture(), any[Class[SystemModel]])

    filter = new VersioningFilter(configurationService, metricsServiceOpt)
    filter.init(new MockFilterConfig())

    systemModelListener = systemModelListenerCaptor.getValue
    versioningListener = versioningListenerCaptor.getValue

    systemModelListener.configurationUpdated(systemModel)

    reset(configurationService)
  }

  describe("init") {
    it("should subscribe to the configuration service") {
      filter.init(new MockFilterConfig())

      verify(configurationService).subscribeTo(anyString(), anyString(), any[URL], any[UpdateListener[ServiceVersionMappingList]], any[Class[ServiceVersionMappingList]])
      verify(configurationService).subscribeTo(anyString(), any[UpdateListener[SystemModel]], any[Class[SystemModel]])
    }
  }

  describe("destroy") {
    it("should unsubscribe from the configuration service") {
      filter.destroy()

      verify(configurationService, times(2)).unsubscribeFrom(anyString(), any[UpdateListener[_]])
    }
  }

  describe("doFilter") {
    it("should throw a 503 if the filter has not yet initialized") {
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
    }

    it("should return on request for service root") {
      versioningListener.configurationUpdated(createDefaultVersioningConfig())

      request.setRequestURI("/")
      request.addHeader(HttpHeaders.ACCEPT, MimeType.APPLICATION_XML.toString)

      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
      response.getStatus shouldBe HttpServletResponse.SC_OK
    }

    it("should return on request for version root") {
      versioningListener.configurationUpdated(createDefaultVersioningConfig())

      request.setRequestURI("/v1")
      request.addHeader(HttpHeaders.ACCEPT, MimeType.APPLICATION_XML.toString)

      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
      response.getStatus shouldBe HttpServletResponse.SC_OK
    }

    it("should return multiple choices") {
      versioningListener.configurationUpdated(createDefaultVersioningConfig())

      request.setRequestURI("/nothingwerecognize")
      request.addHeader(HttpHeaders.ACCEPT, MimeType.APPLICATION_XML.toString)

      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
      response.getStatus shouldBe HttpServletResponse.SC_MULTIPLE_CHOICES
    }

    it("should pass request") {
      versioningListener.configurationUpdated(createDefaultVersioningConfig())

      request.setRequestURI("/v1/somethingelse")
      request.addHeader(HttpHeaders.ACCEPT, MimeType.APPLICATION_XML.toString)

      filter.doFilter(request, response, filterChain)

      filterChain.getRequest should not be null
      filterChain.getResponse should not be null
    }

    it("should catch bad mapping to host") {
      versioningListener.configurationUpdated(createDefaultVersioningConfig())

      request.setRequestURI("/v3/somethingelse")
      request.addHeader(HttpHeaders.ACCEPT, MimeType.APPLICATION_XML.toString)

      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
      response.getStatus shouldBe HttpServletResponse.SC_BAD_GATEWAY
    }

    it("should set accept from media type parameter") {
      versioningListener.configurationUpdated(createDefaultVersioningConfig())

      request.setRequestURI("/somethingthere")
      request.addHeader(HttpHeaders.ACCEPT, "application/vnd.vendor.service-v1+xml")

      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(HttpHeaders.ACCEPT) shouldBe MimeType.APPLICATION_XML.toString
    }
  }

  def createDefaultVersioningConfig(): ServiceVersionMappingList = {
    new ServiceVersionMappingList()
      .withVersionMapping(
        new ServiceVersionMapping()
          .withId("/v1")
          .withPpDestId("endpoint")
          .withMediaTypes(
            new MediaTypeList()
              .withMediaType(
                new MediaType()
                  .withBase("application/xml")
                  .withType("application/vnd.vendor.service-v1+xml")
              )
          ),
        new ServiceVersionMapping()
          .withId("/v2")
          .withPpDestId("endpoint")
          .withMediaTypes(
            new MediaTypeList()
              .withMediaType(
                new MediaType()
                  .withBase("application/xml")
                  .withType("application/vnd.vendor.service-v2+xml")
              )
          ),
        new ServiceVersionMapping()
          .withId("/v3")
          .withPpDestId("badHost")
          .withMediaTypes(
            new MediaTypeList()
              .withMediaType(
                new MediaType()
                  .withBase("application/xml")
                  .withType("application/vnd.vendor.service-v3+xml")
              )
          )
      )
  }
}
