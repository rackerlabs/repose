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
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class HeaderTranslationFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  val mockFilterChain = mock[FilterChain]

  var filter: HeaderTranslationFilter = _

  override def beforeEach() = {
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

    val headerFive = new Header
    headerFive.setOriginalName("X-Five")
    headerFive.getNewName.add("X-New-Five-One")
    headerFive.setRemoveOriginal(false)
    headerFive.setQuality(null)
    headerFive.setSplittable(true)

    val headerSix = new Header
    headerSix.setOriginalName("X-Six")
    headerSix.getNewName.add("X-New-Six-One")
    headerSix.setRemoveOriginal(false)
    headerSix.setQuality(null)
    headerSix.setSplittable(false)

    val headerSeven = new Header
    headerSeven.setOriginalName("X-Seven")
    headerSeven.getNewName.add("X-New-Seven-One")
    headerSeven.setRemoveOriginal(false)
    headerSeven.setQuality(0.71)
    headerSeven.setSplittable(true)

    val headerEight = new Header
    headerEight.setOriginalName("X-Eight")
    headerEight.getNewName.add("X-New-Eight-One")
    headerEight.setRemoveOriginal(false)
    headerEight.setQuality(0.82)
    headerEight.setSplittable(false)

    val headerNine = new Header
    headerNine.setOriginalName("X-Nine")
    headerNine.getNewName.add("X-New-Nine")
    headerNine.setOverwriteTarget(true)

    config.getHeader.add(headerOne)
    config.getHeader.add(headerTwo)
    config.getHeader.add(headerThree)
    config.getHeader.add(headerFour)
    config.getHeader.add(headerFive)
    config.getHeader.add(headerSix)
    config.getHeader.add(headerSeven)
    config.getHeader.add(headerEight)
    config.getHeader.add(headerNine)

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

      getCapturedRequest.getHeaderNames.toSeq should contain theSameElementsAs request.getHeaderNames.toSeq
    }

    it("should remove the original header") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeader("X-One") shouldBe null
    }

    it("should add a new header with a single value") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-One-One").toSeq should contain ("valueOne")
    }

    it("should add new headers with multiple values") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val capturedRequest = getCapturedRequest
      capturedRequest.getHeaders("X-New-Two-One").toSeq should contain theSameElementsAs Seq("valueOne", "valueTwo")
      capturedRequest.getHeaders("X-New-Two-Two").toSeq should contain theSameElementsAs Seq("valueOne", "valueTwo")
    }

    it("should preserve header value order") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val capturedRequest = getCapturedRequest
      capturedRequest.getHeaders("X-New-One-One").toSeq should contain ("valueOne")
      capturedRequest.getHeaders("X-New-Two-One").toSeq should contain inOrderOnly ("valueOne", "valueTwo")
      capturedRequest.getHeaders("X-New-Two-Two").toSeq should contain inOrderOnly ("valueOne", "valueTwo")
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

      getCapturedRequest.getHeaderNames.toSeq should contain theSameElementsAs request.getHeaderNames.toSeq
    }

    it("should not remove the original headers") {
      filter.doFilter(mockRequest, null, mockFilterChain)

      val capturedRequest = getCapturedRequest
      capturedRequest.getHeaders("X-Three").toSeq should contain ("valueOne")
      capturedRequest.getHeaders("X-Four").toSeq should contain theSameElementsAs Seq("valueOne", "valueTwo")
    }
  }

  describe("splittable header configuration") {
    it("should not split the headers when NOT configured to do so") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Six", "mustard,ketchup,mayo;q=0.9,pickles;q=0.1")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Six-One").toSeq should contain theSameElementsAs Seq("mustard,ketchup,mayo;q=0.9,pickles;q=0.1")
    }

    it("should split the headers when configured") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Five", "mustard,ketchup,mayo;q=0.9,pickles;q=0.1")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Five-One").toSeq should contain inOrderOnly ("mustard", "ketchup", "mayo;q=0.9", "pickles;q=0.1")
    }

    it("should correctly handle multiple splittable headers") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Five", "mustard,ketchup,mayo;q=0.9,pickles;q=0.1")
      mockRequest.addHeader("X-Five", "bacon;q=1.0,cheese;q=0.8")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Five-One").toSeq should contain inOrderOnly ("mustard", "ketchup", "mayo;q=0.9", "pickles;q=0.1", "bacon;q=1.0", "cheese;q=0.8")
    }
  }

  describe("when configured to set the header quality") {
    it("sets the configured quality on the new header") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Eight", "mustard")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Eight-One").toSeq should contain ("mustard;q=0.82")
    }

    it("removes the original quality before adding the new header") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Eight", "mayo;q=1.0")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Eight-One").toSeq should contain ("mayo;q=0.82")
    }

    it("correctly handles non-splittable headers") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Eight", "mustard,mayo,cheese")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Eight-One").toSeq should contain ("mustard,mayo,cheese;q=0.82")
    }

    it("correctly handles splittable headers") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Seven", "mustard,mayo,cheese")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Seven-One").toSeq should contain theSameElementsAs Seq("mustard;q=0.71", "mayo;q=0.71", "cheese;q=0.71")
    }

    it("removes the original quality on splittable headers before adding the new header") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Seven", "mustard,mayo;q=0.2,cheese")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Seven-One").toSeq should contain theSameElementsAs Seq("mustard;q=0.71", "mayo;q=0.71", "cheese;q=0.71")
    }
  }

  describe("when configured to overwrite the target") {
    it("correctly overwrites the target header") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Nine", "banana")
      mockRequest.addHeader("X-New-Nine", "phone")

      filter.doFilter(mockRequest, null, mockFilterChain)

      getCapturedRequest.getHeaders("X-New-Nine").toSeq should contain theSameElementsAs Seq("banana")
    }
  }

  def getCapturedRequest: HttpServletRequest = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
    verify(mockFilterChain).doFilter(requestCaptor.capture(), any[ServletResponse])
    requestCaptor.getValue
  }
}
