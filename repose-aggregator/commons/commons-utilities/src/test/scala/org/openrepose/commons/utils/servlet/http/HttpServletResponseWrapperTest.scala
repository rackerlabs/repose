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
package org.openrepose.commons.utils.servlet.http

import java.io.{ByteArrayInputStream, IOException, UnsupportedEncodingException}
import java.nio.charset.UnsupportedCharsetException
import javax.servlet.http.HttpServletResponse
import javax.servlet.{ServletOutputStream, ServletResponse}
import javax.ws.rs.core.HttpHeaders._

import org.apache.http.client.utils.DateUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mEq, _}
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletResponse

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class HttpServletResponseWrapperTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  val modePermutations: Array[(ResponseMode, ResponseMode)] =
    for (headerMode <- ResponseMode.values(); bodyMode <- ResponseMode.values()) yield (headerMode, bodyMode)

  val modePermutationsMutable: Seq[(ResponseMode, ResponseMode)] =
  for (headerMode <- ResponseMode.values(); bodyMode <- ResponseMode.values()
    if headerMode == ResponseMode.MUTABLE || bodyMode == ResponseMode.MUTABLE)
      yield (headerMode, bodyMode)

  var originalResponse: MockHttpServletResponse = _
  var listAppender: ListAppender = _

  override def beforeEach(): Unit = {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
    originalResponse = new MockHttpServletResponse()
  }

  describe("getResponse") {
    it("should throw an UnsupportedOperationException") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[UnsupportedOperationException] should be thrownBy wrappedResponse.getResponse
    }
  }

  describe("setResponse") {
    it("should throw an UnsupportedOperationException") {
      val mockServletResponse = mock[ServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[UnsupportedOperationException] should be thrownBy wrappedResponse.setResponse(mockServletResponse)
    }
  }

  describe("getHeaderNames") {
    it("should not return any header names added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeaderNames should not contain "a"
    }

    it("should return all header names added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNames should contain only("a", "b")
    }

    it("should not return any header names removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeaderNames should contain only "b"
    }

    it("should return header names with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNames should contain only("A", "b")
    }
  }

  describe("getPreferredHeaders") {
    it("should throw a QualityFormatException if the quality is not parseable") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a;q=fish")

      a[QualityFormatException] should be thrownBy wrappedResponse.getPreferredHeaders("a")
    }

    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getPreferredHeaders("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a", 0.8)
      wrappedResponse.addHeader("a", "b", 0.8)
      wrappedResponse.addHeader("a", "c", 0.4)

      wrappedResponse.getPreferredHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getPreferredHeaders("a") shouldBe 'empty
    }

    it("should not return query parameters in the header value") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")
      wrappedResponse.addHeader("A", "b;bar=baz,c")

      wrappedResponse.getPreferredHeaders("A") should contain only "b"
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getPreferredHeaders("a") should contain only("a", "B")
    }
  }

  describe("getPreferredHeadersWithParameters") {
    it("should throw a QualityFormatException if the quality is not parseable") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a;q=fish")

      a[QualityFormatException] should be thrownBy wrappedResponse.getPreferredHeadersWithParameters("a")
    }

    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getPreferredHeadersWithParameters("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a", 0.8)
      wrappedResponse.addHeader("a", "b", 0.8)
      wrappedResponse.addHeader("a", "c", 0.4)

      wrappedResponse.getPreferredHeadersWithParameters("a") should contain only("a;q=0.8", "b;q=0.8")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getPreferredHeadersWithParameters("a") shouldBe 'empty
    }

    it("should return query parameters in the header value") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")
      wrappedResponse.addHeader("A", "b;bar=baz,c")

      wrappedResponse.getPreferredHeadersWithParameters("A") should contain only "b;bar=baz,c"
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getPreferredHeadersWithParameters("a") should contain only("a", "B")
    }
  }

  describe("getHeaderNamesList") {
    it("should not return any header names added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeaderNamesList should not contain "a"
    }

    it("should return all header names added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNamesList should contain only("a", "b")
    }

    it("should not return any header names removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeaderNamesList should contain only "b"
    }

    it("should return header names with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNamesList should contain only("A", "b")
    }
  }

  describe("containsHeader") {
    it("should not contain any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.containsHeader("a") shouldBe false
    }

    it("should contain all headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.containsHeader("a") shouldBe true
      wrappedResponse.containsHeader("b") shouldBe true
    }

    it("should not contain any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")
      wrappedResponse.removeHeader("a")

      wrappedResponse.containsHeader("a") shouldBe false
      wrappedResponse.containsHeader("b") shouldBe true
    }

    it("should contain headers irrespective of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")

      wrappedResponse.containsHeader("A") shouldBe true
      wrappedResponse.containsHeader("a") shouldBe true
    }
  }

  describe("getHeader") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should return a header added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should return a header value added in PASSTHROUGH mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }

    it("should return a header value added in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }

    it("should return a header value set in PASSTHROUGH mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }

    it("should return a header value set in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }

    it("should return the full header value, with query parameters included") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")

      wrappedResponse.getHeader("A") shouldEqual "a;q=0.8;foo=bar"
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")
      wrappedResponse.addHeader("b", "B")

      wrappedResponse.getHeader("A") shouldEqual "a"
      wrappedResponse.getHeader("b") shouldEqual "B"
    }

    it("should return the first header value if multiple exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }
  }

  describe("getHeaders") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeaders("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeaders("a") shouldBe 'empty
    }

    it("should return the full headers value, with query parameters included") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")
      wrappedResponse.addHeader("A", "b;bar=baz")

      wrappedResponse.getHeaders("A") should contain only("a;q=0.8;foo=bar", "b;bar=baz")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getHeaders("a") should contain only("a", "B")
    }
  }

  describe("getSplittableHeaders") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getSplittableHeaders("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getSplittableHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getSplittableHeaders("a") shouldBe 'empty
    }

    it("should trim the space around the comma") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a , b")

      wrappedResponse.getSplittableHeaders("A") should contain only("a", "b")
    }

    it("should return the full header values, with query parameters included") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar,b;bar=baz")

      wrappedResponse.getSplittableHeaders("A") should contain only("a;q=0.8;foo=bar", "b;bar=baz")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getSplittableHeaders("a") should contain only("a", "B")
    }

    it("should return a comma-serparated header value as multiple header values") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a,b")

      wrappedResponse.getSplittableHeaders("a").size() shouldEqual 2
      wrappedResponse.getSplittableHeaders("a") should (contain("a") and contain("b"))
    }
  }

  describe("getPreferredSplittableHeaders") {
    it("should throw a QualityFormatException if the quality is not parseable") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a;q=fish")

      a[QualityFormatException] should be thrownBy wrappedResponse.getPreferredHeaders("a")
    }

    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getPreferredSplittableHeaders("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getPreferredSplittableHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getPreferredSplittableHeaders("a") shouldBe 'empty
    }

    it("should not return query parameters in the header value") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar,b;bar=baz")

      wrappedResponse.getPreferredSplittableHeaders("A") should contain only "b"
    }

    it("should return split headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a", 0.8)
      wrappedResponse.appendHeader("a", "b", 0.8)
      wrappedResponse.appendHeader("a", "c", 0.4)

      wrappedResponse.getPreferredSplittableHeaders("a") should contain only("a", "b")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getPreferredSplittableHeaders("a") should contain only("a", "B")
    }

    it("should return a comma-serparated header value as multiple header values") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a,b")

      wrappedResponse.getPreferredSplittableHeaders("a").size() shouldEqual 2
      wrappedResponse.getPreferredSplittableHeaders("a") should (contain("a") and contain("b"))
    }
  }

  describe("getPreferredSplittableHeadersWithParameters") {
    it("should throw a QualityFormatException if the quality is not parseable") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a;q=fish")

      a[QualityFormatException] should be thrownBy wrappedResponse.getPreferredHeadersWithParameters("a")
    }

    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") shouldBe 'empty
    }

    it("should return query parameters in the header value") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar,b;bar=baz")

      wrappedResponse.getPreferredSplittableHeadersWithParameters("A") should contain only "b;bar=baz"
    }

    it("should return split headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a", 0.8)
      wrappedResponse.appendHeader("a", "b", 0.8)
      wrappedResponse.appendHeader("a", "c", 0.4)

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") should contain only("a;q=0.8", "b;q=0.8")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") should contain only("a", "B")
    }

    it("should return a comma-serparated header value as multiple header values") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a,b")

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a").size() shouldEqual 2
      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") should (contain("a") and contain("b"))
    }
  }

  describe("addHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b")

      verify(mockResponse).addHeader("b", "b")
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b")

      verify(mockResponse).addHeader("b", "b")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should add a value to a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header with a quality when provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b", 0.5)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=0.5"
    }

    it("should add a header with a quality, even if the quality is 1.0") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b", 1.0)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=1.0"
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.addHeader("a", "b")

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*addHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: b"
    }
  }

  describe("addIntHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("b", 2)

      verify(mockResponse).addHeader("b", "2")
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("b", 2)

      verify(mockResponse).addHeader("b", "2")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("b", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "2"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)

      wrappedResponse.getHeaders("a") should contain only "1"
    }

    it("should add a value to a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.addIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("1", "2")
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.addIntHeader("A", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("1", "2")
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "1"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.removeHeader("a")
      wrappedResponse.addIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "2"
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.addIntHeader("a", 1)

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*addIntHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: 1"
    }
  }

  describe("addDateHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addDateHeader("b", System.currentTimeMillis())

      verify(mockResponse).addHeader(mEq("b"), anyString())
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.addDateHeader("b", System.currentTimeMillis())

      verify(mockResponse).addHeader(mEq("b"), anyString())
    }

    it("should add a new header, in a RFC2616 compliant format, if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.commitToResponse()

      DateUtils.parseDate(originalResponse.getHeader("a")) should not be null
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should add a value to a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.addDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.addDateHeader("A", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()

      wrappedResponse.removeHeader("a")
      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.removeHeader("a")
      wrappedResponse.addDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.addDateHeader("a", System.currentTimeMillis())

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*addDateHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: "
    }
  }

  describe("appendHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH and the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "b")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY and the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "b")
    }

    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH and a quality is passed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a", 0.5)
    }

    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH, a quality is passed, and the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "b", 0.5)
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY and a quality is passed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a", 0.5)
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY, a quality is passed, and the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "b", 0.5)
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should append a value if the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "a")
      wrappedResponse.appendHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") should (include("a") and include("b"))
    }

    it("should append a value to the end of the first value if the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.appendHeader("a", "c")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
      originalResponse.getHeaders("a") should contain("a,c")
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "a")
      wrappedResponse.appendHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") should (include("a") and include("b"))
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.appendHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.appendHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header with a quality when provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("b", "b", 0.5)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=0.5"
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.appendHeader("a", "b")

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*appendHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: b"
    }
  }

  describe("replaceHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b")

      verify(mockResponse).setHeader("b", "b")
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b")

      verify(mockResponse).setHeader("b", "b")
    }

    it("should write through to the wrapped response if header mode is PASSTHROUGH and a quality is passed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b", 0.5)

      verify(mockResponse).setHeader(mEq("b"), anyString())
    }

    it("should write through to the wrapped response if header mode is READONLY and a quality is passed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b", 0.5)

      verify(mockResponse).setHeader(mEq("b"), anyString())
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should treat preceding interactions as unwritable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should overwrite the value if a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "a")
      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "a")
      wrappedResponse.replaceHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header with a quality when provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b", 0.5)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=0.5"
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.replaceHeader("a", "b")

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*replaceHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: b"
    }
  }

  describe("getHeadersList") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeadersList("a") shouldBe 'empty
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeadersList("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeadersList("a") shouldBe 'empty
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getHeadersList("a") should contain only("a", "B")
    }
  }

  describe("setHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("b", "b")

      verify(mockResponse).setHeader("b", "b")
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("b", "b")

      verify(mockResponse).setHeader("b", "b")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.setHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should treat preceding interactions as unwritable, add a new header if it does not exist") {
      originalResponse.setHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should overwrite the value if a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")
      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")
      wrappedResponse.setHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.setHeader("a", "b")

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*setHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: b"
    }
  }

  describe("setIntHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("b", 2)

      verify(mockResponse).setHeader("b", "2")
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("b", 2)

      verify(mockResponse).setHeader("b", "2")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("b", 1)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "1"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.setIntHeader("a", 1)

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 2)

      wrappedResponse.getHeaders("a") should contain only "2"
    }

    it("should treat preceding interactions as unwritable, add a new header if it does not exist") {
      originalResponse.setIntHeader("a", 1)

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("1", "2")
    }

    it("should overwrite the value if a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 1)
      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "2"
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 1)
      wrappedResponse.setIntHeader("A", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "2"
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "2"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 1)
      wrappedResponse.removeHeader("a")
      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "2"
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.setIntHeader("a", 1)

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*setIntHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: 1"
    }
  }

  describe("setDateHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setDateHeader("b", System.currentTimeMillis())

      verify(mockResponse).setHeader(mEq("b"), anyString())
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setDateHeader("b", System.currentTimeMillis())

      verify(mockResponse).setHeader(mEq("b"), anyString())
    }

    it("should add a new header, in a RFC2616 compliant format, if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.commitToResponse()

      DateUtils.parseDate(originalResponse.getHeader("a")) should not be null
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should overwrite the value of a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.setDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.setDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()

      wrappedResponse.removeHeader("a")
      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.removeHeader("a")
      wrappedResponse.setDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.setDateHeader("a", System.currentTimeMillis())

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*setDateHeader.* after the response has been committed may be ignored -- the following header may not be modified: a: "
    }
  }

  describe("removeHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.removeHeader("a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.removeHeader("a")
    }

    it("should not remove a header from the wrapped response") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldBe "a"
    }

    it("should remove a header that has been added") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should remove a header in a case-insensitive way") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("A")

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)
      wrappedResponse.removeHeader("a")

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        "Calls to .*removeHeader.* after the response has been committed may be ignored -- the following header may not be modified: a"
    }
  }

  describe("getOutputStreamAsInputStream") {
    it("should throw an IllegalStateException if the body mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.getOutputStreamAsInputStream
    }

    it("should return an input stream containing the contents of the output stream if the body mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      val body = "test body"
      wrappedResponse.getOutputStream.print(body)

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }

    it("should return an input stream containing the contents of the output stream if the body mode is set to MUTABLE") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      val body = "test body"
      wrappedResponse.getOutputStream.print(body)

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }
  }

  describe("getOutputStreamAsString") {
    it("should throw an IllegalStateException if the body mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.getOutputStreamAsString
    }

    it("should return a string containing the contents of the output stream if the body mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      val body = "test body"
      wrappedResponse.getOutputStream.print(body)

      val wrappedBody = wrappedResponse.getOutputStreamAsString

      wrappedBody shouldEqual body
    }

    it("should return a string containing the contents of the output stream if the body mode is set to MUTABLE") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      val body = "test body"
      wrappedResponse.getOutputStream.print(body)

      val wrappedBody = wrappedResponse.getOutputStreamAsString

      wrappedBody shouldEqual body
    }
  }

  describe("setOutput") {
    it("should set the output") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      val body = "test body"
      wrappedResponse.setOutput(new ByteArrayInputStream(body.getBytes))

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }

    it("should set the output, clearing any previous output") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)
      wrappedResponse.getOutputStream.print("foo")

      val body = "test body"
      wrappedResponse.setOutput(new ByteArrayInputStream(body.getBytes))

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }

    it("should set the output, allowing for a new write interface to be used") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)
      wrappedResponse.getOutputStream.print("foo")

      val body = "test body"
      wrappedResponse.setOutput(new ByteArrayInputStream(body.getBytes))

      noException should be thrownBy wrappedResponse.getWriter
    }
  }

  describe("getWriter") {
    it("should print to the wrapped output stream") {
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE, out)

      val body = "foo"
      wrappedResponse.getWriter.print(body)
      wrappedResponse.getWriter.flush()
      wrappedResponse.commitToResponse()

      out.toString shouldEqual body
    }

    it("should not allow a call to getPrintWriter after getOutputStream") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.getOutputStream
      a[IllegalStateException] should be thrownBy wrappedResponse.getWriter
    }

    it("should throw an UnsupportedEncodingException if getCharacterEncoding returns an unsupported encoding") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.setHeader(CONTENT_TYPE, "foo; charset=bar")

      an[UnsupportedEncodingException] should be thrownBy wrappedResponse.getWriter
    }
  }

  describe("getOutputStream") {
    it("should return a passthrough output stream type if the body mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getOutputStream shouldBe a[PassthroughServletOutputStream]
    }

    it("should return a read-only output stream type if the body mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      wrappedResponse.getOutputStream shouldBe a[ReadOnlyServletOutputStream]
    }

    it("should return a mutable output stream type if the body mode is set to MUTABLE") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.getOutputStream shouldBe a[MutableServletOutputStream]
    }

    it("should print to the wrapped output stream") {
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE, out)

      val body = "foo"
      wrappedResponse.getOutputStream.print(body)
      wrappedResponse.commitToResponse()

      out.toString shouldEqual body
    }

    it("should not allow a call to getOutputStream after getPrintWriter") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.getWriter
      a[IllegalStateException] should be thrownBy wrappedResponse.getOutputStream
    }
  }

  describe("setContentLength") {
    it("should make the content length header visible by getHeader(s) in PASSTHROUGH mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CONTENT_LENGTH) shouldEqual "100"
    }

    it("should make the content length header visible by getHeader(s) in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CONTENT_LENGTH) shouldEqual "100"
    }

    it("should make the content length header visible by getHeader(s) in MUTABLE mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CONTENT_LENGTH) shouldEqual "100"
    }

    it("should not set the Content-Length header and log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CONTENT_LENGTH) shouldBe null

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        s"Calls to .*setContentLength.* after the response has been committed may be ignored -- the following header may not be modified: $CONTENT_LENGTH: 100"
    }
  }

  describe("getContentType") {
    it("should return null if no content type has been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getContentType shouldBe null
    }

    it("should return the content type if it has been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getContentType shouldEqual "text/plain"
    }
  }

  describe("setContentType") {
    it("should make the content type header visible by getHeader(s) in PASSTHROUGH mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldEqual "text/plain"
    }

    it("should make the content type header visible by getHeader(s) in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldEqual "text/plain"
    }

    it("should make the content type header visible by getHeader(s) in MUTABLE mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldEqual "text/plain"
    }

    it("should set the content type with a character encoding if one has been provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain;charset=UTF-8")

      wrappedResponse.getContentType should (include("text/plain") and include("charset=UTF-8"))
    }

    it("should set the content type without a character encoding if one has been provided after calling getWriter") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getWriter
      wrappedResponse.setContentType("text/plain;charset=UTF-8")

      wrappedResponse.getContentType shouldEqual "text/plain"
    }

    it("should not set the Content-Type header and log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldBe null

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        s"Calls to .*setContentType.* after the response has been committed may be ignored -- the following header may not be modified: $CONTENT_TYPE: text/plain"
    }

    it("should not set the Content-Type header and log a warning if getWriter has already been called") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.getWriter()
      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldBe null

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        s"Calls to .*setContentType.* after the response has been committed may be ignored -- the following header may not be modified: $CONTENT_TYPE: text/plain"
    }
  }

  describe("getCharacterEncoding") {
    it("should return ISO-8859-1 if no content type has been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getCharacterEncoding shouldBe "ISO-8859-1"
    }

    it("should return the character encoding if it has been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getCharacterEncoding shouldEqual "UTF-8"
    }
  }

  describe("setCharacterEncoding") {
    it("should throw an UnsupportedEncodingException if the provided encoding is not supported") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[UnsupportedCharsetException] should be thrownBy wrappedResponse.setCharacterEncoding("foo")
    }

    it("should make the character encoding visible in the Content-Type header in PASSTHROUGH mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CONTENT_TYPE) should include("charset=UTF-8")
    }

    it("should make the character encoding visible in the Content-Type header in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CONTENT_TYPE) should include("charset=UTF-8")
    }

    it("should make the character encoding visible in the Content-Type header in MUTABLE mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CONTENT_TYPE) should include("charset=UTF-8")
    }

    it("should override an existing character encoding") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-16")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CONTENT_TYPE) should (include("charset=UTF-8") and not include "charset=UTF-16")
    }

    it("should not modify the Content-Type header if it has not been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldBe null
    }

    it("should not set the character encoding after calling getWriter") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.getWriter
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getContentType shouldEqual "text/plain"
    }

    it("should not set the character encoding and log a warning if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CONTENT_TYPE) shouldBe null

      val logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex
        s"Calls to .*setCharacterEncoding.* after the response has been committed may be ignored -- the following header may not be modified: $CONTENT_TYPE: \\*;charset=UTF-8"
    }
  }

  describe("flushBuffer") {
    it("should flush all written data to the underlying output stream") {
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE, out)

      val body = "foo"
      wrappedResponse.getOutputStream.print(body)
      wrappedResponse.flushBuffer()
      wrappedResponse.commitToResponse()

      out.toString shouldEqual body
    }

    it("should flush all written data to the underlying output stream via PrintWriter") {
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE, out)

      val body = "foo"
      wrappedResponse.getWriter.print(body)
      wrappedResponse.flushBuffer()
      wrappedResponse.commitToResponse()

      out.toString shouldEqual body
    }

    it("should flush the wrapped response if not in a mutable mode") {
      val mockResponse = new MockHttpServletResponse() {
        var committed = false

        override def flushBuffer(): Unit = committed = true

        override def isCommitted: Boolean = committed
      }
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setStatus(418)
      wrappedResponse.setHeader("foo", "bar")
      wrappedResponse.getWriter.print("foo")
      wrappedResponse.flushBuffer()

      wrappedResponse.isCommitted shouldBe true
      mockResponse.getStatus shouldEqual 418
      mockResponse.getHeader("foo") shouldEqual "bar"
    }
  }

  describe("resetBuffer") {
    it("should throw an IllegalStateException if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      an[IllegalStateException] should be thrownBy wrappedResponse.resetBuffer()
    }

    it("should clear the contents of the response body without clearing the status code or headers") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.setOutput(new ByteArrayInputStream("foo".getBytes))
      wrappedResponse.addHeader("foo", "foo")
      wrappedResponse.setStatus(418)

      wrappedResponse.resetBuffer()

      val postFlushBody = Source.fromInputStream(wrappedResponse.getOutputStreamAsInputStream).mkString
      wrappedResponse.getStatus shouldEqual 418
      wrappedResponse.getHeader("foo") shouldEqual "foo"
      postFlushBody shouldEqual ""
    }

    it("should reset the response body type") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.getWriter

      wrappedResponse.resetBuffer()

      // no exception should be thrown
      wrappedResponse.getOutputStream
    }
  }

  describe("reset") {
    it("should throw an IllegalStateException if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      an[IllegalStateException] should be thrownBy wrappedResponse.reset()
    }

    it("should clear the contents of the response body, status code, and headers") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.setOutput(new ByteArrayInputStream("foo".getBytes))
      wrappedResponse.addHeader("foo", "foo")
      wrappedResponse.setStatus(418)

      wrappedResponse.reset()

      val postFlushBody = Source.fromInputStream(wrappedResponse.getOutputStreamAsInputStream).mkString
      wrappedResponse.getStatus shouldEqual originalResponse.getStatus
      wrappedResponse.getHeader("foo") shouldBe null
      postFlushBody shouldEqual ""
    }
  }

  describe("commitToResponse") {
    modePermutations.diff(modePermutationsMutable) foreach { case (headerMode, bodyMode) =>
      it(s"should throw an IllegalStateException if the header mode is set to ${headerMode.name()} and body mode is set to ${bodyMode.name()}") {
        val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

        an[IllegalStateException] should be thrownBy wrappedResponse.commitToResponse()
      }
    }

    it("should not alter pre-existing headers in the wrapped response") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "a"
      originalResponse.getHeaders("b") should contain only "b"
    }

    it("should write a modified body through the desired output stream") {
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE, out)

      val body = "foo"
      wrappedResponse.getOutputStream.print(body)

      wrappedResponse.commitToResponse()

      out.toString shouldEqual body
    }

    it("should throw an IOException if the underlying stream does") {
      val out = mock[ServletOutputStream]
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE, out)

      when(out.write(any[Array[Byte]], anyInt(), anyInt())).thenThrow(new IOException())

      an[IOException] should be thrownBy wrappedResponse.commitToResponse()
    }

    it("should flush the wrapped response if flushBuffer has been called") {
      val mockResponse = new MockHttpServletResponse() {
        var committed = false

        override def flushBuffer(): Unit = committed = true

        override def isCommitted: Boolean = committed
      }
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.flushBuffer()
      wrappedResponse.commitToResponse()

      mockResponse.isCommitted shouldBe true
      wrappedResponse.isCommitted shouldBe true
    }

    it("should write the content-length header") {
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE, out)

      val body = "foo"
      wrappedResponse.getOutputStream.print(body)

      wrappedResponse.commitToResponse()

      originalResponse.getHeader(CONTENT_LENGTH) shouldEqual "3"
    }

    it("should mark the wrapped response as committed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.commitToResponse()

      wrappedResponse.isCommitted shouldBe true
    }

    it("should call sendError on the wrapped response if sendError was called (one argument)") {
      val errorCode = 404
      val mockResponse = mock[HttpServletResponse]
      val mockOutputStream = mock[ServletOutputStream]

      when(mockResponse.getStatus).thenReturn(200)
      when(mockResponse.getOutputStream).thenReturn(mockOutputStream)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.getOutputStream.print("test")
      wrappedResponse.addHeader("test", "test")
      wrappedResponse.sendError(errorCode)
      wrappedResponse.commitToResponse()

      verifyZeroInteractions(mockOutputStream)
      verify(mockResponse, never).setContentLength(anyInt)
      verify(mockResponse).addHeader("test", "test")
      verify(mockResponse).sendError(errorCode)
    }

    it("should call sendError on the wrapped response if sendError was called (two arguments)") {
      val errorCode = 404
      val errorMsg = "lorem ipsum"
      val mockResponse = mock[HttpServletResponse]
      val mockOutputStream = mock[ServletOutputStream]

      when(mockResponse.getStatus).thenReturn(200)
      when(mockResponse.getOutputStream).thenReturn(mockOutputStream)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.getOutputStream.print("test")
      wrappedResponse.addHeader("test", "test")
      wrappedResponse.sendError(errorCode, errorMsg)
      wrappedResponse.commitToResponse()

      verifyZeroInteractions(mockOutputStream)
      verify(mockResponse, never).setContentLength(anyInt)
      verify(mockResponse).addHeader("test", "test")
      verify(mockResponse).sendError(errorCode, errorMsg)
    }

    it("should call setStatus on the wrapped response if sendError was not called (one argument)") {
      val statusCode = 201
      val mockResponse = mock[HttpServletResponse]
      val mockOutputStream = mock[ServletOutputStream]

      when(mockResponse.getStatus).thenReturn(200)
      when(mockResponse.getOutputStream).thenReturn(mockOutputStream)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.setStatus(statusCode)
      wrappedResponse.commitToResponse()

      verify(mockResponse).setStatus(statusCode)
    }

    it("should call setStatus on the wrapped response if sendError was not called (two arguments)") {
      val statusCode = 201
      val reasonPhrase = "lorem ipsum"
      val mockResponse = mock[HttpServletResponse]
      val mockOutputStream = mock[ServletOutputStream]

      when(mockResponse.getStatus).thenReturn(200)
      when(mockResponse.getOutputStream).thenReturn(mockOutputStream)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.setStatus(statusCode, reasonPhrase)
      wrappedResponse.commitToResponse()

      verify(mockResponse).setStatus(statusCode, reasonPhrase)
    }
  }

  describe("setStatus") {
    it("should throw an exception if the wrapped response has already been committed (one argument)") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()

      an[IllegalStateException] should be thrownBy wrappedResponse.setStatus(418)
    }

    it("should throw an exception if the wrapped response has already been committed (two arguments)") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()

      an[IllegalStateException] should be thrownBy wrappedResponse.setStatus(418, "TEAPOT")
    }

    it("should set the status code and reason") {
      val mockResponse = mock[HttpServletResponse]
      when(mockResponse.isCommitted).thenReturn(false)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setStatus(418, "TEAPOT")

      verify(mockResponse).setStatus(418, "TEAPOT")
      wrappedResponse.getReason shouldEqual "TEAPOT"
    }

    it("should reset the reason on subsequent calls with no reason") {
      val mockResponse = mock[HttpServletResponse]
      when(mockResponse.isCommitted).thenReturn(false)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setStatus(418, "TEAPOT")
      wrappedResponse.setStatus(200)

      verify(mockResponse).setStatus(418, "TEAPOT")
      verify(mockResponse).setStatus(200)
      wrappedResponse.getReason shouldBe null
    }

    modePermutationsMutable foreach { case (headerMode, bodyMode) =>
      it(s"should not immediately call through to the underlying response with $headerMode header mode and $bodyMode body mode (one argument)") {
        val mockResponse = mock[HttpServletResponse]
        when(mockResponse.isCommitted).thenReturn(false)

        val wrappedResponse = new HttpServletResponseWrapper(mockResponse, headerMode, bodyMode)

        wrappedResponse.setStatus(418)

        verify(mockResponse, never).setStatus(anyInt())
        verify(mockResponse, never).setStatus(anyInt(), anyString())
        wrappedResponse.getStatus shouldEqual 418
        wrappedResponse.getReason shouldBe null
      }

      it(s"should not immediately call through to the underlying response with $headerMode header mode and $bodyMode body mode (two arguments)") {
        val mockResponse = mock[HttpServletResponse]
        when(mockResponse.isCommitted).thenReturn(false)

        val wrappedResponse = new HttpServletResponseWrapper(mockResponse, headerMode, bodyMode)

        wrappedResponse.setStatus(418, "TEAPOT")

        verify(mockResponse, never).setStatus(anyInt())
        verify(mockResponse, never).setStatus(anyInt(), anyString())
        wrappedResponse.getStatus shouldEqual 418
        wrappedResponse.getReason shouldBe "TEAPOT"
      }
    }
  }

  describe("getStatus") {
    modePermutations foreach { case (headerMode, bodyMode) =>
      it(s"should return the status code of the underlying response with headerMode: $headerMode, bodyMode: $bodyMode") {
        val mockResponse = mock[HttpServletResponse]
        when(mockResponse.getStatus).thenReturn(418)

        val wrappedResponse = new HttpServletResponseWrapper(mockResponse, headerMode, bodyMode)

        wrappedResponse.getStatus shouldEqual 418
      }

      it(s"should return the set status code with headerMode: $headerMode, bodyMode: $bodyMode") {
        val mockResponse = mock[HttpServletResponse]
        when(mockResponse.getStatus).thenReturn(418)

        val wrappedResponse = new HttpServletResponseWrapper(mockResponse, headerMode, bodyMode)
        wrappedResponse.setStatus(814)

        wrappedResponse.getStatus shouldEqual 814
      }
    }
  }

  describe("sendError") {
    it("should throw an exception if the wrapped response has already been committed (one argument)") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()

      an[IllegalStateException] should be thrownBy wrappedResponse.sendError(418)
    }

    it("should throw an exception if the wrapped response has already been committed (two arguments)") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()

      an[IllegalStateException] should be thrownBy wrappedResponse.sendError(418, "TEAPOT")
    }

    modePermutationsMutable.foreach {
      case (headerMode, bodyMode) =>
        it(s"should not throw an exception if the wrapped response has already been committed when using the force option with headerMode $headerMode and bodyMode $bodyMode (one argument)") {
          val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

          wrappedResponse.sendError(418)

          wrappedResponse.isCommitted shouldBe true

          wrappedResponse.sendError(814, true)

          wrappedResponse.getStatus shouldEqual 814
        }

        it(s"should not throw an exception if the wrapped response has already been committed when using the force option with headerMode $headerMode and bodyMode $bodyMode (two arguments)") {
          val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

          wrappedResponse.sendError(418, "TEAPOT")

          wrappedResponse.isCommitted shouldBe true

          wrappedResponse.sendError(814, "TOPEAT", true)

          wrappedResponse.getStatus shouldEqual 814
        }
    }

    it("should set the status code and reason") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      // A hack to make this test work -- the original response /should/ set the status when sendError is called, but
      // this mock doesn't. So I set it here. If sendError overwrites it like it should, great, if not, fine.
      originalResponse.setStatus(418)

      wrappedResponse.sendError(418, "TEAPOT")

      wrappedResponse.getStatus shouldBe 418
      wrappedResponse.getReason shouldEqual "TEAPOT"
    }

    it("should reset the reason on subsequent calls with no reason") {
      val mockResponse = mock[HttpServletResponse]
      when(mockResponse.isCommitted).thenReturn(false)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setStatus(418, "TEAPOT")
      wrappedResponse.sendError(404)

      verify(mockResponse).setStatus(418, "TEAPOT")
      verify(mockResponse).sendError(404)
      wrappedResponse.getReason shouldBe null
    }

    it("should reset the buffer") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)

      wrappedResponse.getWriter.write("foo")
      wrappedResponse.getWriter.flush()
      wrappedResponse.sendError(404)

      wrappedResponse.getOutputStreamAsString shouldEqual ""
    }

    it("should reset the content-length in mutable mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)
      val msg = "foo"

      wrappedResponse.setContentLength(msg.length)
      wrappedResponse.getWriter.write(msg)
      wrappedResponse.getWriter.flush()
      wrappedResponse.sendError(404)

      wrappedResponse.getHeader(CONTENT_LENGTH) shouldBe null
    }

    it("should mark the wrapped as committed when given a single argument") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418)

      wrappedResponse.isCommitted shouldBe true
    }

    it("should mark the wrapper as committed when given two arguments") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418, "TEAPOT")

      wrappedResponse.isCommitted shouldBe true
    }

    it("should call through to the underlying response if not in a mutable header mode (one argument)") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418)

      verify(mockResponse).sendError(418)
    }

    it("should call through to the underlying response if not in a mutable header mode (two arguments)") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418, "TEAPOT")

      verify(mockResponse).sendError(418, "TEAPOT")
    }

    it("should not commit the underlying response if in a mutable header mode") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.MUTABLE,
        ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418, "TEAPOT")

      verify(mockResponse, never()).sendError(anyInt(), anyString())
    }

    it("should commit the underlying response if not in a mutable body mode") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418, "TEAPOT")

      verify(mockResponse).sendError(anyInt(), anyString())
    }

    it("should not write, not commit the underlying response if in a mutable body mode") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.MUTABLE)

      wrappedResponse.sendError(418, "TEAPOT")

      verify(mockResponse, never()).sendError(anyInt(), anyString())
    }
  }

  describe("isError") {
    modePermutations foreach { case (headerMode, bodyMode) =>
      it(s"should return false if sendError has not been called with header mode: $headerMode, body mode: $bodyMode") {
        val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

        wrappedResponse.isError shouldBe false
      }

      it(s"should return true if sendError has been called (one argument) with header mode: $headerMode, body mode: $bodyMode") {
        val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

        wrappedResponse.sendError(404)

        wrappedResponse.isError shouldBe true
      }

      it(s"should return true if sendError has been called (two arguments) with header mode: $headerMode, body mode: $bodyMode") {
        val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

        wrappedResponse.sendError(404, "floop")

        wrappedResponse.isError shouldBe true
      }
    }
  }

  describe("getReason") {
    it("should return null if it has not been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getReason shouldBe null
    }

    it("should return the reason string if it has been set by setStatus") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setStatus(418, "TEAPOT")

      wrappedResponse.getReason shouldEqual "TEAPOT"
    }

    it("should return the reason string if it has been set by sendError") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(418, "TEAPOT")

      wrappedResponse.getReason shouldEqual "TEAPOT"
    }
  }

  describe("isCommitted") {
    it("should return false if nothing has been committed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.isCommitted shouldBe false
    }

    it("should return true if the underlying response has been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.isCommitted shouldBe true
    }

    it("should return true after sendError(i) is called") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(HttpServletResponse.SC_NOT_FOUND)

      wrappedResponse.isCommitted shouldBe true
    }

    it("should return true after sendError(i, s) is called") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Stuff was not found")

      wrappedResponse.isCommitted shouldBe true
    }

    it("should return true after flushBuffer is called") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()

      wrappedResponse.isCommitted shouldBe true
    }

    it("should return true after commitToResponse is called") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.commitToResponse()

      wrappedResponse.isCommitted shouldBe true
    }

    it("should throw an exception after uncommit is called while in PASSTHROUGH mode following a sendError on a non-wrapped HttpServletResponse") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.sendError(HttpServletResponse.SC_NOT_FOUND)
      val exception = the[IllegalStateException] thrownBy wrappedResponse.uncommit()

      exception.getMessage shouldBe "Cannot call uncommit after the response has been committed"
    }

    modePermutationsMutable.foreach {
      case (headerMode, bodyMode) =>
        it(s"should return false after uncommit is called with headerMode $headerMode and bodyMode $bodyMode") {
          val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

          wrappedResponse.sendError(HttpServletResponse.SC_NOT_FOUND)
          wrappedResponse.uncommit()

          wrappedResponse.isCommitted shouldBe false
        }
    }
  }

  describe("uncommit") {
    it("should throw an exception if the wrapped response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      an[IllegalStateException] should be thrownBy wrappedResponse.uncommit()
    }

    it("should throw an exception after uncommit is called while in PASSTHROUGH mode following a flushBuffer on a non-wrapped HttpServletResponse") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()
      val exception = the[IllegalStateException] thrownBy wrappedResponse.uncommit()

      exception.getMessage shouldBe "Cannot call uncommit after the response has been committed"
    }

    modePermutationsMutable.foreach {
      case (headerMode, bodyMode) =>
        it(s"should enable calling methods normally blocked by committing with headerMode $headerMode and bodyMode $bodyMode") {
          val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

          wrappedResponse.flushBuffer()
          wrappedResponse.uncommit()

          // should not throw an exception
          wrappedResponse.resetBuffer()
        }
    }
  }

  describe("resetError") {
    it("should throw an exception if the wrapped response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      an[IllegalStateException] should be thrownBy wrappedResponse.resetError()
    }

    it("should prevent calling through to sendError on the wrapped response") {
      val mockResponse = mock[HttpServletResponse]

      when(mockResponse.getOutputStream).thenReturn(mock[ServletOutputStream])

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      wrappedResponse.sendError(404, "lorem ipsum")
      wrappedResponse.uncommit()
      wrappedResponse.resetError()
      wrappedResponse.commitToResponse()

      verify(mockResponse, never()).sendError(anyInt())
      verify(mockResponse, never()).sendError(anyInt(), anyString())
    }
  }
}
