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

  def addHeaderRequestConfig(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.setQuality(0.2)

    conf.setRequest(new HttpMessage)
    conf.getRequest.getHeader.add(header)

    conf
  }

  def addHeaderResponseConfig(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.setQuality(0.2)

    conf.setResponse(new HttpMessage)
    conf.getResponse.getHeader.add(header)

    conf
  }

  def addHeaderRequestConfigRemoveOriginal(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.setQuality(0.2)
    header.setRemoveOriginal(true)

    conf.setRequest(new HttpMessage)
    conf.getRequest.getHeader.add(header)

    conf
  }

  def addHeaderResponseConfigRemoveOriginal(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.setQuality(0.2)
    header.setRemoveOriginal(true)

    conf.setResponse(new HttpMessage)
    conf.getResponse.getHeader.add(header)

    conf
  }

  def addHeaderRequestWithMultipleValuesConfig(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.getValue.add("newer-value")
    header.getValue.add("newest-value")
    header.setQuality(0.2)

    conf.setRequest(new HttpMessage)
    conf.getRequest.getHeader.add(header)

    conf
  }

  def addHeaderResponseWithMultipleValuesConfig(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.getValue.add("newer-value")
    header.getValue.add("newest-value")
    header.setQuality(0.2)

    conf.setResponse(new HttpMessage)
    conf.getResponse.getHeader.add(header)

    conf
  }

  def addMultipleHeadersRequestWithMultipleValuesConfig(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val headerOne = new Header()
    headerOne.setName("x-new-header")
    headerOne.getValue.add("new-value")
    headerOne.getValue.add("newer-value")
    headerOne.getValue.add("newest-value")
    headerOne.setQuality(0.2)

    conf.setRequest(new HttpMessage)
    conf.getRequest.getHeader.add(headerOne)

    val headerTwo = new Header()
    headerTwo.setName("x-newer-header")
    headerTwo.getValue.add("newish-value")
    headerTwo.getValue.add("newerish-value")
    headerTwo.getValue.add("newestish-value")
    headerTwo.setQuality(0.5)

    conf.getRequest.getHeader.add(headerTwo)

    conf
  }

  def addMultipleHeadersResponseWithMultipleValuesConfig(): AddHeadersConfig = {
    val conf = new AddHeadersConfig

    val headerOne = new Header()
    headerOne.setName("x-new-header")
    headerOne.getValue.add("new-value")
    headerOne.getValue.add("newer-value")
    headerOne.getValue.add("newest-value")
    headerOne.setQuality(0.2)

    conf.setResponse(new HttpMessage)
    conf.getResponse.getHeader.add(headerOne)

    val headerTwo = new Header()
    headerTwo.setName("x-newer-header")
    headerTwo.getValue.add("newish-value")
    headerTwo.getValue.add("newerish-value")
    headerTwo.getValue.add("newestish-value")
    headerTwo.setQuality(0.5)

    conf.getResponse.getHeader.add(headerTwo)

    conf
  }

  describe("Handle request by adding headers") {
    it("should contain added headers") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestConfig())

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with removing original") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestConfigRemoveOriginal())

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 1
    }

    it("should contain added header with multiple values") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderRequestWithMultipleValuesConfig())

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-other-header")) shouldBe false
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header")) shouldBe true
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("newer-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).size() shouldEqual 3
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with multiple values") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addMultipleHeadersRequestWithMultipleValuesConfig())

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-newer-header")) shouldBe true
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header")) shouldBe true
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("newer-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).size() shouldEqual 3
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-newer-header")).contains("newish-value;q=0.5")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-newer-header")).contains("newerish-value;q=0.5")
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-newer-header")).size() shouldEqual 3
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
      handler = new AddHeaderHandler(addHeaderResponseConfig())

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header"))
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with removing original") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addHeaderResponseConfigRemoveOriginal())

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header"))
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 1
    }

    it("should contain added header with multiple values") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addHeaderResponseWithMultipleValuesConfig())

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-other-header")) shouldBe false
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header")) shouldBe true
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("newer-value;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).size() shouldEqual 3
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with multiple values") {
      val mockResponse = new ReadableResponseWrapper(new MockHttpServletResponse())
      handler = new AddHeaderHandler(addMultipleHeadersResponseWithMultipleValuesConfig())

      myDirector = handler.handleResponse(null, mockResponse)
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-newer-header")) shouldBe true
      myDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header")) shouldBe true
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("newer-value;q=0.2")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).size() shouldEqual 3
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-newer-header")).contains("newish-value;q=0.5")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-newer-header")).contains("newerish-value;q=0.5")
      myDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap("x-newer-header")).size() shouldEqual 3
      myDirector.responseHeaderManager().headersToAdd().size() shouldEqual 2
      myDirector.responseHeaderManager().headersToRemove().size() shouldEqual 0
    }
  }
}
