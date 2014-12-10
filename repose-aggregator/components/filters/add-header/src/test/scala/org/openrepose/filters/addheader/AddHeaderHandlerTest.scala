package org.openrepose.filters.addheader

import com.mockrunner.mock.web._
import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.filters.addheader.config.{AddHeaderType, Header}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConverters._

/**
 * Created by dimi5963 on 12/4/14.
 */
@RunWith(classOf[JUnitRunner])
class AddHeaderHandlerTest extends FunSpec with Matchers with PrivateMethodTester with BeforeAndAfter {
  var handler: AddHeaderHandler = _
  var myDirector: FilterDirector = _

  def addHeaderConfig(): List[Header] = {
    val conf = new AddHeaderType

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.setQuality(0.2)

    conf.getHeader.add(header)

    conf.getHeader.asScala.toList
  }

  def addHeaderConfigRemoveOriginal(): List[Header] = {
    val conf = new AddHeaderType

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.setQuality(0.2)
    header.setRemoveOriginal(true)

    conf.getHeader.add(header)

    conf.getHeader.asScala.toList
  }

  def addHeaderWithMultipleValuesConfig(): List[Header] = {
    val conf = new AddHeaderType

    val header = new Header()
    header.setName("x-new-header")
    header.getValue.add("new-value")
    header.getValue.add("newer-value")
    header.getValue.add("newest-value")
    header.setQuality(0.2)

    conf.getHeader.add(header)

    conf.getHeader.asScala.toList
  }

  def addMultipleHeadersWithMultipleValuesConfig() = {
    val conf = new AddHeaderType

    val headerOne = new Header()
    headerOne.setName("x-new-header")
    headerOne.getValue.add("new-value")
    headerOne.getValue.add("newer-value")
    headerOne.getValue.add("newest-value")
    headerOne.setQuality(0.2)

    conf.getHeader.add(headerOne)

    val headerTwo = new Header()
    headerTwo.setName("x-newer-header")
    headerTwo.getValue.add("newish-value")
    headerTwo.getValue.add("newerish-value")
    headerTwo.getValue.add("newestish-value")
    headerTwo.setQuality(0.5)

    conf.getHeader.add(headerTwo)

    conf.getHeader.asScala.toList
  }

  describe("Handle request by adding headers") {
    it("should contain added headers") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderConfig())

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 0
    }

    it("should contain added headers with removing original") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderConfigRemoveOriginal())

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-new-header"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-new-header")).contains("new-value;q=0.2")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToRemove().size() shouldEqual 1
    }

    it("should contain added header with multiple values") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")
      handler = new AddHeaderHandler(addHeaderWithMultipleValuesConfig())

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
      handler = new AddHeaderHandler(addMultipleHeadersWithMultipleValuesConfig())

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

}
