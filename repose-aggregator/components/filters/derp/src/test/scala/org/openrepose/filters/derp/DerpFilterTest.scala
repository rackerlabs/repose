package org.openrepose.filters.derp

import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletResponse}

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.FunSpec

import scala.collection.JavaConverters.asJavaEnumerationConverter

class DerpFilterTest extends FunSpec {

  describe("doFilter") {
    it("should pass the request if no delegation header is present") {
      val derpFilter = new DerpFilter()
      val req = mockRequest(Map())
      val fc = mock(classOf[FilterChain])

      derpFilter.doFilter(req, null, fc)

      verify(fc).doFilter(same(req), any(classOf[ServletResponse]))
    }

    it("should send an error response populated with the data from the delegation header if present") {
      val derpFilter = new DerpFilter()
      val req = mockRequest(Map("X-Delegated" -> Seq("status_code=404`component=foo`message=not found;q=1.0")))
      val resp = mock(classOf[HttpServletResponse])

      derpFilter.doFilter(req, resp, null)

      verify(resp).sendError(404, "not found")
    }

    it("should send an error response corresponding to the delegation value with the highest quality") {
      val derpFilter = new DerpFilter()
      val req = mockRequest(Map("X-Delegated" -> Seq("status_code=404`component=foo`message=not found;q=0.8", "status_code=500`component=foo`message=bar;q=0.9")))
      val resp = mock(classOf[HttpServletResponse])

      derpFilter.doFilter(req, resp, null)

      verify(resp).sendError(500, "bar")
    }

    it("should forward the request if no delegation value could be parsed") {
      val derpFilter = new DerpFilter()
      val req = mockRequest(Map("X-Delegated" -> Seq("status_code=404`component=foo`message=not found`q=1.0")))
      val fc = mock(classOf[FilterChain])

      derpFilter.doFilter(req, null, fc)

      verify(fc).doFilter(same(req), any(classOf[ServletResponse]))
    }

    it("should treat a delegation value without an explicit quality as having a quality of 1")(pending)
  }

  describe("parseDelegationValues") {
    it("should return a sequence of delegation value beans") {
      val derpFilter = new DerpFilter()
      val parsedValues = derpFilter.parseDelegationValues(Seq(
        "status_code=400`component=foo`message=bar;q=0.9",
        "status_code=500`component=foo2`message=baz;q=0.7"
      ))

      assert(parsedValues.size == 2)
      assert(parsedValues.exists(_.statusCode == 400))
      assert(parsedValues.exists(_.statusCode == 500))
    }

    it("should return an empty sequence if not value could be parsed") {
      val derpFilter = new DerpFilter()
      val parsedValues = derpFilter.parseDelegationValues(Seq(
        "status_code=400&component=foo`message=bar;q=0.9",
        "status_code=500`component=foo2`message=baz`q=0.7"
      ))

      assert(parsedValues.size == 0)
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
