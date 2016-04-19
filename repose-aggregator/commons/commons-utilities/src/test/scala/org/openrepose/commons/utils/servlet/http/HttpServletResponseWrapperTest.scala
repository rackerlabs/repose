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

import com.mockrunner.mock.web.MockHttpServletResponse
import org.apache.http.client.utils.DateUtils
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mEq, _}
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class HttpServletResponseWrapperTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  var originalResponse: MockHttpServletResponse = _

  before {
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

      wrappedResponse.getPreferredHeaders("a") should be('empty)
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

      wrappedResponse.getPreferredHeaders("a") should be('empty)
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

      wrappedResponse.getPreferredHeadersWithParameters("a") should be('empty)
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

      wrappedResponse.getPreferredHeadersWithParameters("a") should be('empty)
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

      wrappedResponse.getHeaders("a") should be('empty)
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

      wrappedResponse.getHeaders("a") should be('empty)
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

      wrappedResponse.getSplittableHeaders("a") should be('empty)
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

      wrappedResponse.getSplittableHeaders("a") should be('empty)
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

      wrappedResponse.getPreferredSplittableHeaders("a") should be('empty)
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

      wrappedResponse.getPreferredSplittableHeaders("a") should be('empty)
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

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") should be('empty)
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

      wrappedResponse.getPreferredSplittableHeadersWithParameters("a") should be('empty)
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
  }

  describe("addDateHeader") {
    it("should write through to the wrapped response if header mode is PASSTHROUGH") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.addDateHeader("b", System.currentTimeMillis())

      verify(mockResponse).addHeader(mEq("b"), anyString())
    }

    it("should write through to the wrapped response if header mode is READONLY") {
      val now = System.currentTimeMillis()
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
  }

  describe("getHeadersList") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeadersList("a") should be('empty)
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

      wrappedResponse.getHeadersList("a") should be('empty)
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

      wrappedResponse.setHeader(CommonHttpHeader.CONTENT_TYPE.toString, "foo; charset=bar")

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

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_LENGTH.toString) shouldEqual "100"
    }

    it("should make the content length header visible by getHeader(s) in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_LENGTH.toString) shouldEqual "100"
    }

    it("should make the content length header visible by getHeader(s) in MUTABLE mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_LENGTH.toString) shouldEqual "100"
    }

    it("should not set the Content-Length header if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.setContentLength(100)

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_LENGTH.toString) shouldBe null
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

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) shouldEqual "text/plain"
    }

    it("should make the content type header visible by getHeader(s) in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) shouldEqual "text/plain"
    }

    it("should make the content type header visible by getHeader(s) in MUTABLE mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) shouldEqual "text/plain"
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

    it("should not set the Content-Type header if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.setContentType("text/plain")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) shouldBe null
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

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) should include("charset=UTF-8")
    }

    it("should make the character encoding visible in the Content-Type header in READONLY mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) should include("charset=UTF-8")
    }

    it("should make the character encoding visible in the Content-Type header in MUTABLE mode") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) should include("charset=UTF-8")
    }

    it("should override an existing character encoding") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.setCharacterEncoding("UTF-16")
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) should (include("charset=UTF-8") and not include "charset=UTF-16")
    }

    it("should not modify the Content-Type header if it has not been set") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) shouldBe null
    }

    it("should not set the character encoding after calling getWriter") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setContentType("text/plain")
      wrappedResponse.getWriter
      wrappedResponse.setCharacterEncoding("UTF-8")

      wrappedResponse.getContentType shouldEqual "text/plain"
    }

    it("should not set the character encoding if the response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      wrappedResponse.setCharacterEncoding("text/plain")

      wrappedResponse.getHeader(CommonHttpHeader.CONTENT_TYPE.toString) shouldBe null
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
      wrappedResponse.getStatus shouldEqual 200
      wrappedResponse.getHeader("foo") shouldBe null
      postFlushBody shouldEqual ""
    }
  }

  describe("commitToResponse") {
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

      originalResponse.getHeader(CommonHttpHeader.CONTENT_LENGTH.toString) shouldEqual "3"
    }

    it("should mark the wrapped response as committed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.commitToResponse()

      wrappedResponse.isCommitted shouldBe true
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

    it("should set the status code and message") {
      val mockResponse = mock[HttpServletResponse]
      when(mockResponse.isCommitted).thenReturn(false)

      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.setStatus(418, "TEAPOT")

      verify(mockResponse).setStatus(418, "TEAPOT")
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

    it("should set the status code and body") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      // A hack to make this test work -- the original response /should/ set the status when sendError is called, but
      // this mock doesn't. So I set it here. If sendError overwrites it like it should, great, if not, fine.
      originalResponse.setStatus(418)

      wrappedResponse.sendError(418, "TEAPOT")

      wrappedResponse.getStatus shouldBe 418
      Source.fromInputStream(wrappedResponse.getOutputStreamAsInputStream).mkString shouldEqual "TEAPOT"
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

    it("should write and commit the underlying response if not in a mutable header mode") {
      val mockResponse = mock[HttpServletResponse]
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.PASSTHROUGH,
        out)

      wrappedResponse.sendError(418, "TEAPOT")

      out.baos.toString() shouldEqual "TEAPOT"
      verify(mockResponse).flushBuffer()
    }

    it("should write, not commit the underlying response if in a mutable header mode") {
      val mockResponse = mock[HttpServletResponse]
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.MUTABLE,
        ResponseMode.PASSTHROUGH,
        out)

      wrappedResponse.sendError(418, "TEAPOT")

      out.baos.toString() shouldEqual "TEAPOT"
      verify(mockResponse, never()).flushBuffer()
    }

    it("should write and commit the underlying response if not in a mutable body mode") {
      val mockResponse = mock[HttpServletResponse]
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.PASSTHROUGH,
        out)

      wrappedResponse.sendError(418, "TEAPOT")

      out.baos.toString() shouldEqual "TEAPOT"
      verify(mockResponse).flushBuffer()
    }

    it("should not write, not commit the underlying response if in a mutable body mode") {
      val mockResponse = mock[HttpServletResponse]
      val out = new ByteArrayServletOutputStream()
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse,
        ResponseMode.PASSTHROUGH,
        ResponseMode.MUTABLE,
        out)

      wrappedResponse.sendError(418, "TEAPOT")

      out.baos.toString() shouldEqual ""
      verify(mockResponse, never()).flushBuffer()
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
  }

  describe("uncommit") {
    it("should throw an exception if the wrapped response has already been committed") {
      val mockResponse = mock[HttpServletResponse]
      val wrappedResponse = new HttpServletResponseWrapper(mockResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      when(mockResponse.isCommitted).thenReturn(true)

      an[IllegalStateException] should be thrownBy wrappedResponse.uncommit()
    }

    it("should enable calling methods normally blocked by committing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.flushBuffer()
      wrappedResponse.uncommit()

      // should not throw an exception
      wrappedResponse.resetBuffer()
    }
  }
}
