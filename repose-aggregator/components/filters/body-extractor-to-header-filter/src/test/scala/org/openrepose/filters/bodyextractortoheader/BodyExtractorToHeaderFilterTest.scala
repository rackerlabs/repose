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

import java.net.URL
import java.nio.charset.StandardCharsets
import javax.servlet.FilterConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.Assert.{assertFalse, assertTrue}
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodyextractortoheader.config.{BodyExtractorToHeaderConfig, Extractor}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class BodyExtractorToHeaderFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  val mockConfigurationService = mock[ConfigurationService]
  val mockFilterConfig = mock[FilterConfig]
  val defaultValue = "no-value"
  val nullValue = "null-value"
  val matchValue = "red"
  val stringValue = "String"
  val numberValue = "19.99"
  // From: http://goessner.net/articles/JsonPath/
  val contentBody =
    s"""{ "store": {
        |    "book": [
        |      { "category": "reference",
        |        "author": "Nigel Rees",
        |        "title": "Sayings of the Century",
        |        "price": 8.95
        |      },
        |      { "category": "fiction",
        |        "author": "Evelyn Waugh",
        |        "title": "Sword of Honour",
        |        "price": 12.99
        |      },
        |      { "category": "fiction",
        |        "author": "Herman Melville",
        |        "title": "Moby Dick",
        |        "isbn": "0-553-21311-3",
        |        "price": 8.99
        |      },
        |      { "category": "fiction",
        |        "author": "J. R. R. Tolkien",
        |        "title": "The Lord of the Rings",
        |        "isbn": "0-395-19395-8",
        |        "price": 22.99
        |      }
        |    ],
        |    "bicycle": {
        |      "color": "$matchValue",
        |      "price": 19.95
        |    },
        |    "types": {
        |      "string": "$stringValue",
        |      "number": $numberValue,
        |      "object": {
        |        "val1": "1",
        |        "val2": 2
        |      },
        |      "array": [
        |        { "element1": "val1" },
        |        { "element2": "val2" }
        |      ],
        |      "true": true,
        |      "false": false,
        |      "null": null
        |    },
        |    "regex": {
        |      "group": "hybrid:123456"
        |    }
        |  }
        |}""".stripMargin.getBytes(StandardCharsets.ISO_8859_1)
  val matchPath = "$.store.bicycle.color"
  val badPath = "$.store.bicycle.height"
  val extractedHeader = "X-Extracted-Id"

  var config: BodyExtractorToHeaderConfig = _
  var filter: BodyExtractorToHeaderFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: HttpServletResponse = _
  var filterChain: MockFilterChain = _

  override def beforeEach() = {
    reset(mockConfigurationService)
    config = new BodyExtractorToHeaderConfig
    filter = new BodyExtractorToHeaderFilter(mockConfigurationService)
    servletRequest = new MockHttpServletRequest
    servletResponse = mock[HttpServletResponse]
    filterChain = new MockFilterChain
    servletRequest.setContentType("application/json")
    servletRequest.setContent(contentBody)
  }

  describe("doFilter") {
    it("should add the header with the JPath value when the Body matches the configured JPath and a default is specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, Some(defaultValue), None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe matchValue
    }

    it("should add the header with the default value when the Body does NOT match the configured JPath and a default is specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, badPath, Some(defaultValue), None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe defaultValue
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and a default is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe matchValue
    }

    it("should NOT add the header when the Body does NOT match the configured JPath and a default is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, badPath, None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe null
    }

    it("should add a header for an extractor even if another extractor doesn't add a header") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      config.getExtraction.add(createConfigExtractor("X-Server-Id", "$.bodyData.servers", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe matchValue
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("X-Server-Id") shouldBe null
    }
    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a string") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.string", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe stringValue
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a number") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.number", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe numberValue
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a object") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.object", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe "{val1=1, val2=2}"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a array") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.array", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe """[{"element1":"val1"},{"element2":"val2"}]"""
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a true") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.true", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe "true"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a false") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.false", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe "false"
    }

    it("should add the header with the null value when the Body matches the configured JPath and the value is JSON null") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.null", None, Some(nullValue)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe nullValue
    }

    it("should add the header with the value null when the Body matches the configured JPath and the value is JSON null") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.null", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(extractedHeader).asScala.toTraversable shouldBe empty
    }

    it("should NOT add the header when the Body matches the configured JPath and a null value is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.null", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe null
    }

    it("should NOT add the header when the Body matches the configured JPath and the Content-Type is not JSON") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      filter.configurationUpdated(config)
      servletRequest.setContentType("application/xml")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe null
    }

    it("should NOT add the header when the Body matches the configured JPath and the Content-Type is not set at all") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      filter.configurationUpdated(config)
      servletRequest = new MockHttpServletRequest

      filter.doFilter(servletRequest, servletResponse, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe null
    }

    it("should NOT overwrite the header when the Body matches the configured JPath and Overwrite is FALSE") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None, overwrite = false, None))
      filter.configurationUpdated(config)
      val headerValue = "Ima-Gun Di"
      servletRequest.addHeader(extractedHeader, headerValue)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val headers = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(extractedHeader).asScala.toTraversable
      headers.size shouldBe 2
      headers should contain(headerValue)
      headers should contain(matchValue)
    }

    it("should NOT overwrite the header when the Body does NOT match the configured JPath and Overwrite is TRUE") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, badPath, None, None, overwrite = true, None))
      filter.configurationUpdated(config)
      val headerValue = "Eeth Koth"
      servletRequest.addHeader(extractedHeader, headerValue)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val headers = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(extractedHeader).asScala.toTraversable
      headers.size shouldBe 1
      headers should contain(headerValue)
    }

    it("should overwrite the header when the Body matches the configured JPath and Overwrite is TRUE") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None, overwrite = true, None))
      filter.configurationUpdated(config)
      val headerValue = "Barriss Offee"
      servletRequest.addHeader(extractedHeader, headerValue)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val headers = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(extractedHeader).asScala.toTraversable
      headers.size shouldBe 1
      headers shouldNot contain(headerValue)
      headers should contain(matchValue)
    }

    it("should append a quality to the header value when the Body matches and one is configured") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None, overwrite = true, Some(0.5)))
      filter.configurationUpdated(config)
      val headerValue = "Aayla Secura"
      servletRequest.addHeader(extractedHeader, headerValue)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val headers = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(extractedHeader).asScala.toTraversable
      headers.size shouldBe 1
      headers should contain(s"$matchValue;q=0.5")
    }

    it("should add multiple headers if multiple extractions are satisfied") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None, overwrite = false, None))
      config.getExtraction.add(createConfigExtractor(extractedHeader, badPath, Some(defaultValue), None, overwrite = false, None))
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.string", None, None, overwrite = false, None))
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.number", None, None, overwrite = false, None))
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.null", None, Some(nullValue), overwrite = false, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val headers = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(extractedHeader).asScala.toTraversable
      headers should contain(matchValue)
      headers should contain(defaultValue)
      headers should contain(stringValue)
      headers should contain(numberValue)
      headers should contain(nullValue)
    }

    ignore("The JSON Path library currently doesn't support applying a REGEX capture group expression to a value before returning it.") {
      it("should add the header with the partial JPath value when the Body matches the configured JPath containing a REGEX capture group.") {
        config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.regex.group(^.*:(.*$))", None, None))
        filter.configurationUpdated(config)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(extractedHeader) shouldBe "123456"
      }
    }
  }

  describe("init") {
    it("should subscribe a listener to the configuration service on init") {
      filter.init(mockFilterConfig)

      verify(mockConfigurationService).subscribeTo(
        anyString(),
        anyString(),
        any[URL],
        any(),
        any[Class[BodyExtractorToHeaderConfig]]
      )
    }
  }

  describe("destroy") {
    it("should unsubscribe a listener to the configuration service on destroy") {
      filter.destroy()

      verify(mockConfigurationService).unsubscribeFrom(
        anyString(),
        any()
      )
    }
  }

  describe("initialized") {
    it("should be uninitialized until the configuration is updated") {
      assertFalse(filter.isInitialized)
      filter.configurationUpdated(config)
      assertTrue(filter.isInitialized)
    }
  }

  describe("configuration") {
    it("can be loaded when a default value is specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, Some(defaultValue), None))
      filter.configurationUpdated(config)
    }

    it("can be loaded when a default value is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      filter.configurationUpdated(config)
    }
  }

  def createConfigExtractor(headerName: String,
                            bodyJpath: String,
                            defaultValue: Option[String],
                            nullValue: Option[String]
                           ): Extractor = {
    createConfigExtractor(headerName, bodyJpath, defaultValue, nullValue, overwrite = true, None)
  }

  def createConfigExtractor(headerName: String,
                            bodyJpath: String,
                            defaultValue: Option[String],
                            nullValue: Option[String],
                            overwrite: Boolean,
                            quality: Option[Double]
                           ): Extractor = {
    val extractor = new Extractor
    extractor.setHeader(headerName)
    extractor.setJsonpath(bodyJpath)
    extractor.setDefaultIfMiss(defaultValue match {
      case Some(default) => default
      case None => null
    })
    extractor.setDefaultIfNull(nullValue match {
      case Some(valueNull) => valueNull
      case None => null
    })
    extractor.setOverwrite(overwrite)
    extractor.setQuality(quality match {
      case Some(qual) => qual
      case None => null
    })

    extractor
  }
}
