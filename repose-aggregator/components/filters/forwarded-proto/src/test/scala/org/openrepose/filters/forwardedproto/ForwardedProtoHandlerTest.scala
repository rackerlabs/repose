package org.openrepose.filters.forwardedproto

import java.io.InputStream
import javax.servlet.http.{HttpServletResponse, HttpServletResponseWrapper}



//import com.mockrunner.mock.web._
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.FilterDirector
//import org.openrepose.filters.addheader.config.{AddHeadersConfig, Header, HttpMessage}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}
import com.mockrunner.mock.web.MockHttpServletRequest

class ForwardedProtoHandlerTest extends FunSpec with Matchers with PrivateMethodTester with BeforeAndAfter {

  var handler: ForwardedProtoHandler = _
  var myDirector: FilterDirector = _

  describe("handleRequest") {


    it("the request should contain X-Forwarded-Proto header ") {
      val mockRequest = new MockHttpServletRequest()
      handler = new ForwardedProtoHandler()

      myDirector = handler.handleRequest(mockRequest, null)
      myDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("X-Forwarded-Proto"))
      myDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("X-Forwarded-Proto")).contains("HTTP")
      myDirector.requestHeaderManager().headersToAdd().size() shouldEqual 1
    }

  }


}
