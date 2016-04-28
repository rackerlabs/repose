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
package org.openrepose.filters.bodyextractortoheader

import java.nio.charset.StandardCharsets
import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.filters.bodyextractortoheader.config.{BodyExtractorToHeaderConfig, Extractor}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class BodyExtractorToHeaderFilterTest extends FunSpec with BeforeAndAfter with Matchers {

  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  val deviceValue = "12345678-abcd-fedc-0987-1234567890ab"
  val deviceBody = s"""{bodyData{"some": "stuff", "device": "$deviceValue", "other": "junk"}""".getBytes(StandardCharsets.UTF_8)
  val noMatchBody = s"""{bodyData{"some": "stuff", "noMatch": "$deviceValue", "other": "junk"}""".getBytes(StandardCharsets.UTF_8)
  val devicePath = """$.bodyData.device"""
  val deviceHeader = "X-Device-Id"

  before {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock(classOf[FilterChain])
  }

  describe("doFilter") {
    it("should add the header with the JPath value when the Body matches the configured JPath and a default is specified") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, Some("no-value")))
      val filter = new BodyExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setContent(deviceBody)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(deviceHeader) shouldBe deviceValue
    }

    it("should add the header with the default value when the Body does NOT match the configured JPath and a default is specified") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, Some("no-value")))
      val filter = new BodyExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setContent(noMatchBody)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(deviceHeader) shouldBe "no-value"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and a default is NOT specified") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, None))
      val filter = new BodyExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setContent(deviceBody)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(deviceHeader) shouldBe deviceValue
    }

    it("should NOT add the header when the Body does NOT match the configured JPath and a default is NOT specified") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, None))
      val filter = new BodyExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setContent(noMatchBody)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(deviceHeader) shouldBe null
    }

    it("should add a header for an extractor even if another extractor doesn't add a header") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, Some("no-value")))
      config.getExtraction.add(createConfigExtractor("X-Server-Id", ".*/servers/([^/]+).*", None))
      val filter = new BodyExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setContent(deviceBody)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(deviceHeader) shouldBe deviceValue
      requestCaptor.getValue.getHeader("X-Server-Id") shouldBe null
    }
  }

  describe("configuration") {
    it("can be loaded when a default value is specified") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, Some("no-value")))
      val filter = new BodyExtractorToHeaderFilter(null)

      filter.configurationUpdated(config)
    }

    it("can be loaded when a default value is NOT specified") {
      val config = new BodyExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor(deviceHeader, devicePath, None))
      val filter = new BodyExtractorToHeaderFilter(null)

      filter.configurationUpdated(config)
    }
  }

  def createConfigExtractor(headerName: String, bodyJpath: String, defaultValue: Option[String]): Extractor = {
    val extractor = new Extractor
    extractor.setHeader(headerName)
    extractor.setBodyJpath(bodyJpath)
    extractor.setDefault(defaultValue match {
      case Some(default) => default
      case None => null
    })

    extractor
  }
}
