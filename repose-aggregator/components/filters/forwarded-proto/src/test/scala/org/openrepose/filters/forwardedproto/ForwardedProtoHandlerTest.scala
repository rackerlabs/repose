package org.openrepose.filters.forwardedproto

import javax.servlet.http.{HttpServletResponse, HttpServletResponseWrapper}

import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.core.filter.logic.FilterDirector
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}
import com.mockrunner.mock.web.MockHttpServletRequest

class ForwardedProtoHandlerTest extends FunSpec with Matchers with PrivateMethodTester with BeforeAndAfter {

  var handler: ForwardedProtoHandler = _
  var myDirector: FilterDirector = _

  describe("handleRequest") {


    it("a normal HTTP request should contain X-Forwarded-Proto header with value HTTP/1.1") {
      val mockRequest = new MockHttpServletRequest()
      handler = new ForwardedProtoHandler()

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("X-Forwarded-Proto"))
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("X-Forwarded-Proto")).contains("HTTP/1.1")
    }

    it("an https request should contain X-Forwarded-Proto header with value HTTPS") {
      val mockRequest = new MockHttpServletRequest()
      //may be written differently but for the test it's as long as it is equal to what I set the value to
      mockRequest.setProtocol("HTTPS")
      handler = new ForwardedProtoHandler()

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("X-Forwarded-Proto"))
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("X-Forwarded-Proto")).contains("HTTPS")
    }


  }


}
