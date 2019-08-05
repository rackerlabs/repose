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

package org.openrepose.filters.compression

import java.io.EOFException
import java.util.zip.ZipException
import javax.servlet._

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{doThrow, verify, when}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.external.pjlcompression.CompressingFilter
import org.openrepose.filters.compression.config.{Compression, ContentCompressionConfig}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CompressionFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var filter: CompressionFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  var compressingFilterFactory: CompressingFilterFactory = _
  var compressingFilter: CompressingFilter = _
  var filterConfig: FilterConfig = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock[FilterChain]
    compressingFilterFactory = mock[CompressingFilterFactory]
    compressingFilter = mock[CompressingFilter]
    filterConfig = mock[FilterConfig]

    when(compressingFilterFactory.newCompressingFilter(any(classOf[FilterConfig]))).thenReturn(compressingFilter)
    when(filterConfig.getInitParameterNames).thenReturn(List.empty[String].toIterator.asJavaEnumeration)

    filter = new CompressionFilter(mock[ConfigurationService], compressingFilterFactory)
    filter.init(filterConfig)
  }

  describe("filtering") {
    it("calls the actual filter to do the work") {
      filter.configurationUpdated(defaultConfig)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(compressingFilter).doFilter(any(classOf[ServletRequest]), any(classOf[ServletResponse]), any(classOf[FilterChain]))
    }

    it("returns a 400 on GZIP errors") {
      filter.configurationUpdated(defaultConfig)
      doThrow(new ZipException("Not in GZIP format")).when(compressingFilter).doFilter(any(classOf[ServletRequest]), any(classOf[ServletResponse]), any(classOf[FilterChain]))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getStatus shouldEqual 400
    }

    it("returns a 400 on unexpected EOF errors") {
      filter.configurationUpdated(defaultConfig)
      doThrow(new EOFException).when(compressingFilter).doFilter(any(classOf[ServletRequest]), any(classOf[ServletResponse]), any(classOf[FilterChain]))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getStatus shouldEqual 400
    }

    it("returns a 500 on all other errors") {
      filter.configurationUpdated(defaultConfig)
      doThrow(new ServletException).when(compressingFilter).doFilter(any(classOf[ServletRequest]), any(classOf[ServletResponse]), any(classOf[FilterChain]))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getStatus shouldEqual 500
    }
  }

  describe("updating the configuration") {
    it("will set the config values for the actual filter") {
      val config = defaultConfig

      filter.configurationUpdated(config)

      val createdConfig = getCreatedConfig
      createdConfig.getInitParameter("statsEnabled") shouldBe "false"
      createdConfig.getInitParameter("debug") shouldBe "true"
      createdConfig.getInitParameter("compressionThreshold") shouldBe "42"
    }

    it("will set the Exclude Content Types when it's available and the Include Content Types is not configured") {
      val config = defaultConfig
      config.getCompression.getExcludeContentTypes.addAll(List("application/xml", "image/webp").asJava)

      filter.configurationUpdated(config)

      val createdConfig = getCreatedConfig
      createdConfig.getInitParameter("excludeContentTypes") shouldEqual "application/xml,image/webp"
    }

    it("will not set the Exclude Content Types when the Include Content Types is configured") {
      val config = defaultConfig
      config.getCompression.getExcludeContentTypes.addAll(List("application/xml", "image/webp").asJava)
      config.getCompression.getIncludeContentTypes.addAll(List("application/xhtml+xml", "text/html").asJava)

      filter.configurationUpdated(config)

      val createdConfig = getCreatedConfig
      createdConfig.getInitParameter("includeContentTypes") shouldEqual "application/xhtml+xml,text/html"
      createdConfig.getInitParameter("excludeContentTypes") shouldBe null
    }

    it("will set the Exclude User Agent Patterns when it's available and the Include User Agent Patterns is not configured") {
      val config = defaultConfig
      config.getCompression.getExcludeUserAgentPatterns.addAll(List("Mozilla/5.0", "AppleWebKit").asJava)

      filter.configurationUpdated(config)

      val createdConfig = getCreatedConfig
      createdConfig.getInitParameter("excludeUserAgentPatterns") shouldEqual "Mozilla/5.0,AppleWebKit"
    }

    it("will not set the Exclude User Agent Patterns when the Include User Agent Patterns is configured") {
      val config = defaultConfig
      config.getCompression.getExcludeUserAgentPatterns.addAll(List("Mozilla/5.0", "AppleWebKit").asJava)
      config.getCompression.getIncludeUserAgentPatterns.addAll(List("Gecko/20100101", "Safari/601.3.9").asJava)

      filter.configurationUpdated(config)

      val createdConfig = getCreatedConfig
      createdConfig.getInitParameter("includeUserAgentPatterns") shouldEqual "Gecko/20100101,Safari/601.3.9"
      createdConfig.getInitParameter("excludeUserAgentPatterns") shouldBe null
    }
  }

  def defaultConfig: ContentCompressionConfig = {
    val compression = new Compression
    compression.setStatsEnabled(false)
    compression.setDebug(true)
    compression.setCompressionThreshold(42)

    val contentCompressionConfig = new ContentCompressionConfig
    contentCompressionConfig.setCompression(compression)
    contentCompressionConfig
  }

  def getCreatedConfig: CompressingFilterConfig = {
    val configCaptor = ArgumentCaptor.forClass(classOf[CompressingFilterConfig])
    verify(compressingFilterFactory).newCompressingFilter(configCaptor.capture())
    configCaptor.getValue
  }

}
