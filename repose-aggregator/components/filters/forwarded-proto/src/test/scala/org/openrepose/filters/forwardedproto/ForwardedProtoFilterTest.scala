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

class ForwardedProtoFilterTest extends FunSpec {



  describe("doFilter") {
    it("wat") {

      val protoFilter = new ForwardedProtoFilter();
      val req = mockRequest(Map())
      val fc = mock(classOf[FilterChain])

      protoFilter.doFilter(req, null, fc)




    }


  }

  def mockRequest(headers: Map[String, Iterable[String]]) = {
    val req = mock(classOf[HttpServletRequest])

    when(req.getHeader(anyString())).thenAnswer(new Answer[String] {
      override def answer(invocation: InvocationOnMock): String = {
        if (headers.isEmpty) {
          null
        } else {
          headers(invocation.getArguments()(0).asInstanceOf[String]).head
        }
      }
    })
    when(req.getHeaders(anyString())).thenAnswer(new Answer[util.Enumeration[String]] {
      override def answer(invocation: InvocationOnMock): util.Enumeration[String] = {
        if (headers.isEmpty) {
          Iterator[String]().asJavaEnumeration
        } else {
          headers(invocation.getArguments()(0).asInstanceOf[String]).iterator.asJavaEnumeration
        }
      }
    })
    when(req.getHeaderNames).thenReturn(headers.keysIterator.asJavaEnumeration)

    req
  }
}
