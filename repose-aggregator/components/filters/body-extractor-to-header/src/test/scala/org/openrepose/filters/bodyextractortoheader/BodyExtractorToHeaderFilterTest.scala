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

  var config: BodyExtractorToHeaderConfig = _
  var filter: BodyExtractorToHeaderFilter = _
  var requestCaptor: ArgumentCaptor[MutableHttpServletRequest] = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  // From: http://goessner.net/articles/JsonPath/
  val defaultValue = "no-value"
  val nullValue = "null-value"
  val matchValue = "red"
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
        |      "string": "String",
        |      "number": 19.95,
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
        |    }
        |  }
        |}""".stripMargin.getBytes(StandardCharsets.UTF_8)
  val matchPath = "$.store.bicycle.color"
  val badPath = "$.store.bicycle.height"
  val extractedHeader = "X-Extracted-Id"

  before {
    config = new BodyExtractorToHeaderConfig
    filter = new BodyExtractorToHeaderFilter(null)
    requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock(classOf[FilterChain])
    servletRequest.setContentType("application/json")
    servletRequest.setContent(contentBody)
  }

  describe("doFilter") {
    it("should add the header with the JPath value when the Body matches the configured JPath and a default is specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, Some(defaultValue), None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe matchValue
    }

    it("should add the header with the default value when the Body does NOT match the configured JPath and a default is specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, badPath, Some(defaultValue), None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe defaultValue
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and a default is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe matchValue
    }

    it("should NOT add the header when the Body does NOT match the configured JPath and a default is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, badPath, None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe null
    }

    it("should add a header for an extractor even if another extractor doesn't add a header") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      config.getExtraction.add(createConfigExtractor("X-Server-Id", "$.bodyData.servers", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe matchValue
      requestCaptor.getValue.getHeader("X-Server-Id") shouldBe null
    }
    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a string") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.string", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe "String"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a number") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.number", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe "19.95"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a object") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.object", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe "{val1=1, val2=2}"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a array") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.array", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe """[{"element1":"val1"},{"element2":"val2"}]"""
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a true") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.true", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe "true"
    }

    it("should add the header with the JPath value when the Body matches the configured JPath and the value is a false") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.false", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe "false"
    }

    it("should add the header with the null value when the Body matches the configured JPath and the value is JSON null") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.null", None, Some(nullValue)))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe nullValue
    }

    it("should NOT add the header when the Body matches the configured JPath and a null value is NOT specified") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, "$.store.types.null", None, None))
      filter.configurationUpdated(config)

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe null
    }

    it("should NOT add the header when the Body matches the configured JPath and the Content-Type is not JSON") {
      config.getExtraction.add(createConfigExtractor(extractedHeader, matchPath, None, None))
      filter.configurationUpdated(config)
      servletRequest.setContentType("application/xml")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
      requestCaptor.getValue.getHeader(extractedHeader) shouldBe null
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

  def createConfigExtractor(headerName: String, bodyJpath: String, defaultValue: Option[String], nullValue: Option[String]): Extractor = {
    val extractor = new Extractor
    extractor.setHeader(headerName)
    extractor.setJsonpath(bodyJpath)
    extractor.setDefault(defaultValue match {
      case Some(default) => default
      case None => null
    })
    extractor.setNullValue(nullValue match {
      case Some(valueNull) => valueNull
      case None => null
    })

    extractor
  }
}
