package org.openrepose.filters.forwardedproto

import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{ServletRequest, FilterChain, ServletResponse}

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import org.scalatest.FunSpec

import scala.collection.JavaConverters.asJavaEnumerationConverter

class ForwardedProtoHandlerTest extends FunSpec {



  describe("test") {
    it("wat") {

      val protoFilter = new ForwardedProtoFilter();
      val req = mockRequest(Map())
      val fc = mock(classOf[FilterChain])
      protoFilter.doFilter(req, null, fc)





    }


  }


}
