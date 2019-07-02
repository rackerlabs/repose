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

package org.openrepose.filters.addheader

import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.filters.addheader.config.{AddHeadersConfig, Header, HttpMessage}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class AddHeaderFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import AddHeaderFilterTest._

  var filter: AddHeaderFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock[FilterChain]

    filter = new AddHeaderFilter(null)
  }

  describe("handle request by adding headers") {
    it("should contain added header") {
      filter.configurationUpdated(addHeaderRequestConfig(removeOriginal = false))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("x-new-header-1") should not be null
      postFilterRequest.getHeader("x-new-header-1") should include ("new-value-1;q=0.2")
    }

    it("should contain added header without removing original") {
      filter.configurationUpdated(addHeaderRequestConfig(removeOriginal = false))
      servletRequest.addHeader("x-new-header-1", "old-value-1")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeadersList("x-new-header-1") should not be null
      postFilterRequest.getHeadersList("x-new-header-1") should have size 2
      postFilterRequest.getHeadersList("x-new-header-1") should contain theSameElementsAs List("old-value-1", "new-value-1;q=0.2")
    }

    it("should contain added header after removing original") {
      filter.configurationUpdated(addHeaderRequestConfig(removeOriginal = true))
      servletRequest.addHeader("x-new-header-1", "old-value-1")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("x-new-header-1") should not be null
      postFilterRequest.getHeadersList("x-new-header-1") should have size 1
      postFilterRequest.getHeader("x-new-header-1") should include ("new-value-1;q=0.2")
      postFilterRequest.getHeader("x-new-header-1") should not include "old-value-1"
    }

    it("should contain added header with multiple values") {
      filter.configurationUpdated(addHeaderRequestConfig(removeOriginal = false, numValues = 3))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("x-new-header-1") should not be null
      postFilterRequest.getHeadersList("x-new-header-1") should contain theSameElementsAs List("new-value-1;q=0.2", "new-value-2;q=0.2", "new-value-3;q=0.2")
    }

    it("should contain multiple added headers with multiple values") {
      filter.configurationUpdated(addHeaderRequestConfig(removeOriginal = false, numValues = 3, numHeaders = 2))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader("x-new-header-1") should not be null
      postFilterRequest.getHeadersList("x-new-header-1") should contain theSameElementsAs List("new-value-1;q=0.2", "new-value-2;q=0.2", "new-value-3;q=0.2")
      postFilterRequest.getHeader("x-new-header-2") should not be null
      postFilterRequest.getHeadersList("x-new-header-2") should contain theSameElementsAs List("new-value-4;q=0.2", "new-value-5;q=0.2", "new-value-6;q=0.2")
    }
  }

  describe("handle response by adding headers") {
    it("should contain added header") {
      filter.configurationUpdated(addHeaderResponseConfig(removeOriginal = false))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.containsHeader("x-new-header-1") shouldBe true
      servletResponse.getHeader("x-new-header-1") should include ("new-value-1;q=0.2")
    }

    it("should contain added header without removing original") {
      filter.configurationUpdated(addHeaderResponseConfig(removeOriginal = false))
      addResponseHeaders(List(SimpleHeader("x-new-header-1", "old-value-1")))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.containsHeader("x-new-header-1") shouldBe true
      servletResponse.getHeaders("x-new-header-1") should contain theSameElementsAs List("old-value-1", "new-value-1;q=0.2")
    }

    it("should contain added header after removing original") {
      filter.configurationUpdated(addHeaderResponseConfig(removeOriginal = true))
      addResponseHeaders(List(SimpleHeader("x-new-header-1", "old-value-1"), SimpleHeader("other-header", "some-value")))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.containsHeader("x-new-header-1") shouldBe true
      servletResponse.getHeaders("x-new-header-1") should have size 1
      servletResponse.getHeader("x-new-header-1") should include ("new-value-1;q=0.2")
      servletResponse.getHeader("x-new-header-1") should not include "old-value-1"
      servletResponse.containsHeader("other-header") shouldBe true  // ensure our mock-magic is working
    }

    it("should contain added header with multiple values") {
      filter.configurationUpdated(addHeaderResponseConfig(removeOriginal = false, numValues = 3))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.containsHeader("x-new-header-1") shouldBe true
      servletResponse.getHeaders("x-new-header-1") should contain theSameElementsAs List("new-value-1;q=0.2", "new-value-2;q=0.2", "new-value-3;q=0.2")
    }

    it("should contain multiple added headers with multiple values") {
      filter.configurationUpdated(addHeaderResponseConfig(removeOriginal = false, numValues = 3, numHeaders = 3))

      filter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.containsHeader("x-new-header-1") shouldBe true
      servletResponse.getHeaders("x-new-header-1") should contain theSameElementsAs List("new-value-1;q=0.2", "new-value-2;q=0.2", "new-value-3;q=0.2")
      servletResponse.containsHeader("x-new-header-2") shouldBe true
      servletResponse.getHeaders("x-new-header-2") should contain theSameElementsAs List("new-value-4;q=0.2", "new-value-5;q=0.2", "new-value-6;q=0.2")
    }
  }

  def getPostFilterRequest: HttpServletRequestWrapper = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
    requestCaptor.getValue
  }

  def addResponseHeaders(headers: Iterable[SimpleHeader]): Unit = {
    val responseCaptor = ArgumentCaptor.forClass(classOf[HttpServletResponseWrapper])

    doAnswer(new Answer[Void]() {
      def answer(invocation: InvocationOnMock): Void = {
        headers.foreach { header =>
          // grab the wrapped response that was passed in to the filterChain and add headers to it as if we were the next filter
          responseCaptor.getValue.addHeader(header.name, header.value)
        }
        null
      }
    }).when(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), responseCaptor.capture())
  }

  def addHeaderRequestConfig(removeOriginal: Boolean, numValues: Int = 1, numHeaders: Int = 1): AddHeadersConfig = {
    val conf = new AddHeadersConfig
    conf.setRequest(new HttpMessage)
    createHeaders(removeOriginal, numValues, numHeaders).foreach(conf.getRequest.getHeader.add)

    conf
  }

  def addHeaderResponseConfig(removeOriginal: Boolean, numValues: Int = 1, numHeaders: Int = 1): AddHeadersConfig = {
    val conf = new AddHeadersConfig
    conf.setResponse(new HttpMessage)
    createHeaders(removeOriginal, numValues, numHeaders).foreach(conf.getResponse.getHeader.add)

    conf
  }

  def createHeaders(removeOriginal: Boolean, numValues: Int, numHeaders: Int): IndexedSeq[Header] = {
    for (
      a <- 1 to numHeaders;
      b <- 1 to numValues) yield {

      val header = new Header()
      header.setName("x-new-header-" + a.toString)
      header.setValue("new-value-" + (b + ((a - 1) * numValues)).toString)
      header.setQuality(0.2)
      header.setOverwrite(removeOriginal)

      header
    }
  }
}

object AddHeaderFilterTest {
  case class SimpleHeader(name: String, value: String)
}
