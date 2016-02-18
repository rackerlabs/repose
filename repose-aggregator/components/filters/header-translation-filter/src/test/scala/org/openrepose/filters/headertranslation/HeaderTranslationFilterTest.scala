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
package org.openrepose.filters.headertranslation

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, reset, verify}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.headertranslation.config.{Header, HeaderTranslationType}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class HeaderTranslationFilterTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  val mockFilterChain = mock[FilterChain]

  var filter: HeaderTranslationFilter = _

  before {
    reset(mockFilterChain)

    val configurationService = mock[ConfigurationService]
    filter = new HeaderTranslationFilter(configurationService)

    val config = new HeaderTranslationType

    val headerOne = new Header
    headerOne.setOriginalName("X-One")
    headerOne.getNewName.add("X-New-One-One")
    headerOne.setRemoveOriginal(true)

    val headerTwo = new Header
    headerTwo.setOriginalName("X-Two")
    headerTwo.getNewName.add("X-New-Two-One")
    headerTwo.getNewName.add("X-New-Two-Two")
    headerTwo.setRemoveOriginal(true)

    val headerThree = new Header
    headerThree.setOriginalName("X-Three")
    headerThree.getNewName.add("X-New-Three-One")
    headerThree.setRemoveOriginal(false)

    val headerFour = new Header
    headerFour.setOriginalName("X-Four")
    headerFour.getNewName.add("X-New-Four-One")
    headerFour.getNewName.add("X-New-Four-Two")
    headerFour.setRemoveOriginal(false)

    config.getHeader.add(headerOne)
    config.getHeader.add(headerTwo)
    config.getHeader.add(headerThree)
    config.getHeader.add(headerFour)

    filter.configurationUpdated(config)
  }

  describe("when starting") {
    it("should return a 503 if configuration has not been loaded") {
      val mockConfigService = mock[ConfigurationService]
      val testFilter = new HeaderTranslationFilter(mockConfigService)

      val mockResponse = mock[HttpServletResponse]
      val mockChain = mock[FilterChain]

      testFilter.doFilter(null, mockResponse, mockChain)

      verify(mockChain, never()).doFilter(any(), any())
      verify(mockResponse).sendError(503)
    }
  }

  describe("when configured to remove the original header") {
    val mockRequest = new MockHttpServletRequest()
    mockRequest.addHeader("X-One", "valueOne")
    mockRequest.addHeader("X-Two", "valueOne")
    mockRequest.addHeader("X-Two", "valueTwo")
    mockRequest.addHeader("Accept", "acceptValue")

    it("should not remove any headers if no original headers are found") {
      val request = new MockHttpServletRequest()
      request.addHeader("Accept", "acceptValue")
      request.addHeader("Foo", "bar")

      filter.doFilter(request, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeaderNames.toSeq should contain theSameElementsAs request.getHeaderNames.toSeq
    }

    it("should remove the original header") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeader("X-One") shouldBe null
    }

    it("should add a new header with a single value") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeaders("X-New-One-One").toSeq should contain theSameElementsAs Seq("valueOne")
    }

    it("should add new headers with multiple values") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeaders("X-New-Two-One").toSeq should contain theSameElementsAs Seq("valueOne", "valueTwo")
      capturedRequest.getHeaders("X-New-Two-Two").toSeq should contain theSameElementsAs Seq("valueOne", "valueTwo")
    }

    it("should preserve header value order") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeaders("X-New-One-One").toSeq should contain theSameElementsInOrderAs Seq("valueOne")
      capturedRequest.getHeaders("X-New-Two-One").toSeq should contain theSameElementsInOrderAs Seq("valueOne", "valueTwo")
      capturedRequest.getHeaders("X-New-Two-Two").toSeq should contain theSameElementsInOrderAs Seq("valueOne", "valueTwo")
    }
  }

  describe("when configured to keep the original header name") {
    val mockRequest = new MockHttpServletRequest()
    mockRequest.addHeader("X-Three", "valueOne")
    mockRequest.addHeader("X-Four", "valueOne")
    mockRequest.addHeader("X-Four", "valueTwo")
    mockRequest.addHeader("Accept", "acceptValue")

    it("should not add any headers if no original headers are found") {
      val request = new MockHttpServletRequest()
      request.addHeader("Accept", "acceptValue")
      request.addHeader("Foo", "bar")

      filter.doFilter(request, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeaderNames.toSeq should contain theSameElementsAs request.getHeaderNames.toSeq
    }

    it("should not remove the original headers") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])

      val capturedRequest = requestCaptor.getValue
      capturedRequest.getHeaders("X-Three").toSeq should contain theSameElementsAs Seq("valueOne")
      capturedRequest.getHeaders("X-Four").toSeq should contain theSameElementsAs Seq("valueOne", "valueTwo")
    }
  }
}
