package org.openrepose.filters.contenttypestripper

import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}
import javax.servlet.{ServletRequest, FilterChain, ServletResponse}

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest
import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ContentTypeStripperFilterTest extends FunSpec with Matchers with MockitoSugar {
  describe("The ContentTypeStripperFilter") {
    it("should not modify the content-type header if it and a body exist") {
      val request: MockHttpServletRequest = new MockHttpServletRequest()
      request.addHeader("content-type","application/json")
      request.setContent("some content".getBytes)
      val filterChain: FilterChain = mock[FilterChain]

      (new ContentTypeStripperFilter).doFilter(request, mock[ServletResponse], filterChain)

      val servletRequest: org.mockito.ArgumentCaptor[ServletRequest] = ArgumentCaptor.forClass(classOf[ServletRequest])
      Mockito.verify(filterChain).doFilter(servletRequest.capture(), any())
      val chainRequest = servletRequest.getValue

      chainRequest.isInstanceOf[PushBackHttpServletRequestWrapper] shouldBe true
      IOUtils.toString(chainRequest.getInputStream) shouldBe "some content"
    }
    it("should not return the content-type header if there is no body") {
      val request: MockHttpServletRequest = new MockHttpServletRequest()
      request.addHeader("content-type","application/json")
      request.setContent("".getBytes)
      val filterChain: FilterChain = mock[FilterChain]

      (new ContentTypeStripperFilter).doFilter(request, mock[ServletResponse], filterChain)

      val servletRequest: org.mockito.ArgumentCaptor[ServletRequest] = ArgumentCaptor.forClass(classOf[ServletRequest])
      Mockito.verify(filterChain).doFilter(servletRequest.capture(), any())
      val chainRequest = servletRequest.getValue
      val wrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(chainRequest.asInstanceOf[HttpServletRequest])
      wrapper.getHeaderNames exists (_.equalsIgnoreCase("content-type")) shouldBe false
      wrapper.getHeader("content-type") shouldBe null
    }
  }
}
