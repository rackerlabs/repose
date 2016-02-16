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

package org.openrepose.filters.urlextractortoheader

import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.filters.urlextractortoheader.config.{Extractor, UrlExtractorToHeaderConfig}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class UrlExtractorToHeaderFilterTest extends FunSpec with BeforeAndAfter with Matchers {

  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _

  before {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock(classOf[FilterChain])
  }

  describe("doFilter") {
    it("should add the header with the regex value when the URL matches the configured regex and a default is specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", Some("no-value")))
      val filter = new UrlExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setRequestURI("/v1/hybrid:45678/entities/96")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader("X-Device-Id") shouldBe "hybrid:45678"
    }

    it("should add the header with the default value when the URL does NOT match the configured regex and a default is specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", Some("no-value")))
      val filter = new UrlExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setRequestURI("/v1/potato:45678/entities/96")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader("X-Device-Id") shouldBe "no-value"
    }

    it("should add the header with the regex value when the URL matches the configured regex and a default is NOT specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", None))
      val filter = new UrlExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setRequestURI("/v1/hybrid:45678/entities/96")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader("X-Device-Id") shouldBe "hybrid:45678"
    }

    it("should NOT add the header when the URL does NOT match the configured regex and a default is NOT specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", None))
      val filter = new UrlExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setRequestURI("/v1/potato:45678/entities/96")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader("X-Device-Id") shouldBe null
    }

    it("should add a header for an extractor even if another extractor doesn't add a header") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", Some("no-value")))
      config.getExtraction.add(createConfigExtractor("X-Server-Id", ".*/servers/([^/]+).*", None))
      val filter = new UrlExtractorToHeaderFilter(null)
      filter.configurationUpdated(config)
      servletRequest.setRequestURI("/v1/hybrid:45678/entities/96")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader("X-Device-Id") shouldBe "hybrid:45678"
      requestCaptor.getValue.getHeader("X-Server-Id") shouldBe null
    }
  }

  describe("configuration") {
    it("can be loaded when a default value is specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", Some("no-value")))
      val filter = new UrlExtractorToHeaderFilter(null)

      filter.configurationUpdated(config)
    }

    it("can be loaded when a default value is NOT specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", None))
      val filter = new UrlExtractorToHeaderFilter(null)

      filter.configurationUpdated(config)
    }
  }

  def createConfigExtractor(headerName: String, urlRegex: String, defaultValue: Option[String]): Extractor = {
    val extractor = new Extractor
    extractor.setHeader(headerName)
    extractor.setUrlRegex(urlRegex)
    extractor.setDefault(defaultValue match {
      case Some(default) => default
      case None => null
    })

    extractor
  }
}
