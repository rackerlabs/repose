/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.filters.addheader

import java.io.InputStream
import javax.servlet.http.{HttpServletResponse, HttpServletResponseWrapper}

import com.mockrunner.mock.web._
import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.filters.addheader.config.{AddHeadersConfig, Header, HttpMessage}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

@RunWith(classOf[JUnitRunner])
class AddHeaderHandlerTest extends FunSpec with Matchers with PrivateMethodTester with BeforeAndAfter {
  var handler: AddHeaderHandler = _
  var myDirector: FilterDirector = _

  describe("Handle request by adding headers") {
    it("should contain added headers") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestConfig(false))

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with removing original") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestConfig(true))

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 1
    }

    it("should contain added header with multiple values") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestConfig(false, 3))

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-other-header")) shouldBe false
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1")) shouldBe true
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-2;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-3;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).size() shouldEqual 3
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with multiple values") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestConfig(false, 3, 2))

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-2")) shouldBe true
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1")) shouldBe true
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-2;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-3;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).size() shouldEqual 3
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).contains("new-value-4;q=0.5")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).contains("new-value-5;q=0.5")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).contains("new-value-6;q=0.5")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).size() shouldEqual 3
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 2
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 0
    }
  }

  describe("handle response by adding headers") {
    // note: this is to get around requiring a ReadableHttpServletResponse and using a MutableHttpServletResponse to
    //       satisfy that condition
    class ReadableResponseWrapper(resp: HttpServletResponse) extends HttpServletResponseWrapper(resp) with ReadableHttpServletResponse {
      override def getBufferedOutputAsInputStream: InputStream = null

      override def getMessage: String = null

      override def getInputStream: InputStream = null
    }

    it("should contain added headers") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addHeaderResponseConfig(false))

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1"))
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with removing original") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addHeaderResponseConfig(true))

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1"))
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 1
    }

    it("should contain added header with multiple values") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addHeaderResponseConfig(false, 3))

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-other-header")) shouldBe false
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1")) shouldBe true
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-2;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-3;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).size() shouldEqual 3
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with multiple values") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addHeaderResponseConfig(false, 3, 2))

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-2")) shouldBe true
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header-1")) shouldBe true
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-1;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-2;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).contains("new-value-3;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-1")).size() shouldEqual 3
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).contains("new-value-4;q=0.5")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).contains("new-value-5;q=0.5")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).contains("new-value-6;q=0.5")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header-2")).size() shouldEqual 3
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 2
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 0
    }
  }

  def addHeaderRequestConfig(removeOriginal: Boolean, numValues: Int = 1, numHeaders: Int = 1): AddHeadersConfig = {
    val conf = new AddHeadersConfig
    val header = new Header()
    var headers = List[Header]()

    for (a <- 1 to numHeaders) {
      for (b <- 1 to numValues) {
        val bVal = b + ((a - 1) * numValues)

        val header = new Header()
        header.setName("x-new-header-" + a.toString)
        header.setValue("new-value-" + bVal.toString)
        header.setQuality(0.2)

        if (removeOriginal) {
          header.setOverwrite(true)
        }

        headers = header :: headers
      }
    }

    conf.setRequest(new HttpMessage)
    headers.foreach(conf.getRequest.getHeader.add)

    conf
  }

  def addHeaderResponseConfig(removeOriginal: Boolean, numValues: Int = 1, numHeaders: Int = 1): AddHeadersConfig = {
    val conf = new AddHeadersConfig
    val header = new Header()
    var headers = List[Header]()

    for (a <- 1 to numHeaders) {
      for (b <- 1 to numValues) {
        val bVal = b + ((a - 1) * numValues)

        val header = new Header()
        header.setName("x-new-header-" + a.toString)
        header.setValue("new-value-" + bVal.toString)
        header.setQuality(0.2)

        if (removeOriginal) {
          header.setOverwrite(true)
        }

        headers = header :: headers
      }
    }

    conf.setResponse(new HttpMessage)
    headers.foreach(conf.getResponse.getHeader.add)

    conf
  }
}
