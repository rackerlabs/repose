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
package org.openrepose.filters.openapivalidator

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.atlassian.oai.validator.model.Request
import javax.servlet.http.HttpServletRequest
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.openrepose.filters.openapivalidator.HttpServletOAIRequest.RequestConversionException
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.http.{HttpHeaders, HttpMethod, MediaType}
import org.springframework.mock.web.{DelegatingServletInputStream, MockHttpServletRequest}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HttpServletOAIRequestTest
  extends FunSpec with Matchers with MockitoSugar {

  describe("getPath") {
    it("should return the path from the servlet request") {
      val path = "/test/path"
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, path)

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getPath shouldBe path
    }
  }

  describe("getMethod") {
    Request.Method.values.map(_.name).foreach { method =>
      it(s"should return the $method method from the servlet request") {
        val servletRequest = new MockHttpServletRequest(method, "/")

        val validatorRequest = HttpServletOAIRequest(servletRequest)

        validatorRequest.getMethod shouldBe Request.Method.valueOf(method)
      }
    }

    it("should throw an exception when the servlet request has an unsupported method") {
      val servletRequest = new MockHttpServletRequest("FOO", "/")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      a[RequestConversionException] should be thrownBy validatorRequest.getMethod
    }
  }

  describe("getBody") {
    Set(
      StandardCharsets.ISO_8859_1,
      StandardCharsets.US_ASCII,
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16,
      StandardCharsets.UTF_16BE,
      StandardCharsets.UTF_16LE
    ).foreach { charset =>
      it(s"should return the $charset encoded servlet request body") {
        val requestBody = "Lorem ipsum dolor sit amet, consectetur"
        val contentType = MediaType.TEXT_PLAIN_VALUE
        val servletRequest = new MockHttpServletRequest(HttpMethod.POST.name, "/test/path")
        servletRequest.setContent(requestBody.getBytes(charset))
        servletRequest.setContentType(contentType)
        servletRequest.setCharacterEncoding(charset.name)

        val validatorRequest = HttpServletOAIRequest(servletRequest)

        validatorRequest.getContentType.get shouldBe servletRequest.getHeader(HttpHeaders.CONTENT_TYPE)
        validatorRequest.getBody.get shouldBe requestBody
      }
    }

    it("should return an empty Optional if the servlet request body is empty") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.POST.name, "/test/path")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getContentType.isPresent shouldBe false
      validatorRequest.getBody.isPresent shouldBe false
    }

    Seq(
      "UnsuportedName",
      "&IllegalName"
    ).foreach { characterEncoding =>
      it(s"should throw an exception when the servlet request character encoding is $characterEncoding (unsupported)") {
        val requestBody = "Lorem ipsum dolor sit amet, consectetur"
        val servletRequest = new MockHttpServletRequest(HttpMethod.POST.name, "/test/path")
        servletRequest.setContent(requestBody.getBytes)
        servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE)
        servletRequest.setCharacterEncoding(characterEncoding)

        val validatorRequest = HttpServletOAIRequest(servletRequest)

        a[RequestConversionException] should be thrownBy validatorRequest.getBody
      }
    }

    it("should only read the body once when getBody is called multiple times") {
      val requestBody = "Lorem ipsum dolor sit amet, consectetur"
      val servletRequest = mock[HttpServletRequest]

      when(servletRequest.getInputStream)
        .thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.ISO_8859_1))))

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getBody.get shouldBe requestBody
      noException should be thrownBy validatorRequest.getBody
      validatorRequest.getBody.get shouldBe requestBody
    }
  }

  describe("getQueryParameters") {
    it("should return distinct, case-sensitive query parameter names from the servlet request") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")
      servletRequest.setQueryString("a=1&A=2&b=3&a=4&b=3&A=5,6&c")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getQueryParameters.asScala should contain theSameElementsAs Set("a", "A", "b", "c")
    }

    it("should return an empty collection if the servlet request has no query string") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getQueryParameters.asScala shouldBe empty
    }
  }

  describe("getQueryParameterValues") {
    it("should return query parameter values from the servlet request") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")
      servletRequest.setQueryString("a=1&A=2&b=3&a=4&b=3&A=5,6&c")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getQueryParameterValues("a").asScala should contain theSameElementsAs Seq("1", "4")
      validatorRequest.getQueryParameterValues("A").asScala should contain theSameElementsAs Seq("2", "5,6")
      validatorRequest.getQueryParameterValues("b").asScala should contain theSameElementsAs Seq("3", "3")
      validatorRequest.getQueryParameterValues("c").asScala should contain theSameElementsAs Seq("")
    }

    it("should return an empty collection if the servlet request has no query string") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getQueryParameterValues("a").asScala shouldBe empty
    }
  }

  describe("getHeaders") {
    it("should return a map of headers from the servlet request") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")
      servletRequest.addHeader("a-header", "1")
      servletRequest.addHeader("A-header", "2")
      servletRequest.addHeader("b-header", "3")
      servletRequest.addHeader("a-header", "4")
      servletRequest.addHeader("b-header", "3")
      servletRequest.addHeader("A-header", "5,6")
      servletRequest.addHeader("c-header", "")

      val validatorRequest = HttpServletOAIRequest(servletRequest)
      val headers = validatorRequest.getHeaders.asScala

      headers should contain key "a-header"
      headers should contain key "b-header"
      headers should contain key "c-header"
      headers("a-header") should contain theSameElementsAs Seq("1", "2", "4", "5,6")
      headers("b-header") should contain theSameElementsAs Seq("3", "3")
      headers("c-header") should contain theSameElementsAs Seq("")
    }

    it("should return an empty map if the servlet request has no headers") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getHeaders.asScala shouldBe empty
    }
  }

  describe("getHeaderValues") {
    it("should return a map of headers from the servlet request") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")
      servletRequest.addHeader("a-header", "1")
      servletRequest.addHeader("A-header", "2")
      servletRequest.addHeader("b-header", "3")
      servletRequest.addHeader("a-header", "4")
      servletRequest.addHeader("b-header", "3")
      servletRequest.addHeader("A-header", "5,6")
      servletRequest.addHeader("c-header", "")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getHeaderValues("a-header").asScala should contain theSameElementsAs Seq("1", "2", "4", "5,6")
      validatorRequest.getHeaderValues("a-header").asScala shouldEqual validatorRequest.getHeaderValues("A-hEaDeR").asScala
      validatorRequest.getHeaderValues("b-header").asScala should contain theSameElementsAs Seq("3", "3")
      validatorRequest.getHeaderValues("c-header").asScala should contain theSameElementsAs Seq("")
    }

    it("should return an empty collection if the servlet request has no header of the given name") {
      val servletRequest = new MockHttpServletRequest(HttpMethod.GET.name, "/test/path")

      val validatorRequest = HttpServletOAIRequest(servletRequest)

      validatorRequest.getHeaderValues("a").asScala shouldBe empty
    }
  }
}
