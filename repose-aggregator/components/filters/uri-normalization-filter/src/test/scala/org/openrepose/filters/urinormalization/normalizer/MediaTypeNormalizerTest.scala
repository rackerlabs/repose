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
package org.openrepose.filters.urinormalization.normalizer

import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.urinormalization.config.MediaType
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class MediaTypeNormalizerTest extends FunSpec with BeforeAndAfter with Matchers {

  val configuredMediaTypes = Seq(
    new MediaType().withName("application/xml").withVariantExtension("xml").withPreferred(true)
  )

  var mediaTypeNormalizer: MediaTypeNormalizer = _

  implicit class WithableMediaType(val mt: MediaType) extends MediaType {
    def withName(n: String): WithableMediaType = {
      this.setName(n)
      this
    }

    def withVariantExtension(ve: String): WithableMediaType = {
      this.setVariantExtension(ve)
      this
    }

    def withPreferred(p: Boolean): WithableMediaType = {
      this.setPreferred(p)
      this
    }
  }

  before {
    mediaTypeNormalizer = new MediaTypeNormalizer(configuredMediaTypes)
  }

  describe("getMediaTypeForVariant") {
    it("should correctly capture variant extensions") {
      val request = new MockHttpServletRequest()
      request.setRequestURI("/a/request/uri.xml")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.getMediaTypeForVariant(wrappedRequest)

      wrappedRequest.getRequestURI shouldBe "/a/request/uri"
      wrappedRequest.getRequestURL.toString shouldBe "http://localhost/a/request/uri"
      identifiedMediaType.isDefined shouldBe true
      identifiedMediaType.get.getVariantExtension shouldBe "xml"
    }

    // todo: getRequestUri should not, by contract, return query parameters -- this might be a bad test
    it("should correctly ignore query parameters") {
      val request = new MockHttpServletRequest()
      request.setRequestURI("/a/request/uri.xml?name=name&value=1")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.getMediaTypeForVariant(wrappedRequest)

      wrappedRequest.getRequestURI shouldBe "/a/request/uri?name=name&value=1"
      wrappedRequest.getRequestURL.toString shouldBe "http://localhost/a/request/uri?name=name&value=1"
      identifiedMediaType.isDefined shouldBe true
      identifiedMediaType.get.getVariantExtension shouldBe "xml"
    }

    // todo: again, fragments should not be returned
    it("should correctly ignore uri fragments") {
      val request = new MockHttpServletRequest()
      request.setRequestURI("/a/request/uri.xml#fragment")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.getMediaTypeForVariant(wrappedRequest)

      wrappedRequest.getRequestURI shouldBe "/a/request/uri#fragment"
      wrappedRequest.getRequestURL.toString shouldBe "http://localhost/a/request/uri#fragment"
      identifiedMediaType.isDefined shouldBe true
      identifiedMediaType.get.getVariantExtension shouldBe "xml"
    }

    it("should correctly ignore uri fragments and query parameters") {
      val request = new MockHttpServletRequest()
      request.setRequestURI("/a/request/uri.xml?name=name&value=1#fragment")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.getMediaTypeForVariant(wrappedRequest)

      wrappedRequest.getRequestURI shouldBe "/a/request/uri?name=name&value=1#fragment"
      wrappedRequest.getRequestURL.toString shouldBe "http://localhost/a/request/uri?name=name&value=1#fragment"
      identifiedMediaType.isDefined shouldBe true
      identifiedMediaType.get.getVariantExtension shouldBe "xml"
    }

    it("should correctly capture unusual variant extensions") {
      val request = new MockHttpServletRequest()
      request.setRequestURI("/a/request/uri/.xml")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.getMediaTypeForVariant(wrappedRequest)

      wrappedRequest.getRequestURI shouldBe "/a/request/uri/"
      wrappedRequest.getRequestURL.toString shouldBe "http://localhost/a/request/uri/"
      identifiedMediaType.isDefined shouldBe true
      identifiedMediaType.get.getVariantExtension shouldBe "xml"
    }
  }

  describe("normalizeContentMediaType") {
    it("should set correct media type when wildcard is provided") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.ACCEPT.toString, "*/*")
      request.setRequestURI("/a/request/uri")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.normalizeContentMediaType(wrappedRequest)

      wrappedRequest.getHeader(CommonHttpHeader.ACCEPT.toString) shouldBe "application/xml"
    }

    it("should not set media type when accept is provided") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.ACCEPT.toString, "application/json")
      request.setRequestURI("/a/request/uri")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.normalizeContentMediaType(wrappedRequest)

      wrappedRequest.getHeader(CommonHttpHeader.ACCEPT.toString) shouldBe "application/json"
    }

    it("should set proper media type from extension") {
      val request = new MockHttpServletRequest()
      request.setRequestURI("/a/request/uri.xml")

      val wrappedRequest = new HttpServletRequestWrapper(request)

      val identifiedMediaType = mediaTypeNormalizer.normalizeContentMediaType(wrappedRequest)

      wrappedRequest.getHeader(CommonHttpHeader.ACCEPT.toString) shouldBe "application/xml"
    }
  }
}
