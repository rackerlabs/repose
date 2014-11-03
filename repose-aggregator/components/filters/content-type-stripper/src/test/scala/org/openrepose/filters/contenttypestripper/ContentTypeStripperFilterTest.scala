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
    val shouldReturnParams = List(
      ("body exists", "some content"),
      ("body exists with fewer than 4 characters", "some"),
      ("body exists with white space intermingled", "hi hi")
    )
    shouldReturnParams.foreach { case (testName: String, bodyContent: String) =>
      it(s"should not modify the content-type header if it exists and a non-white space $testName") {
        val request: MockHttpServletRequest = new MockHttpServletRequest()
        request.addHeader("content-type", "application/json")
        request.setContent(bodyContent.getBytes)
        val filterChain: FilterChain = mock[FilterChain]

        (new ContentTypeStripperFilter).doFilter(request, mock[ServletResponse], filterChain)

        val servletRequest: org.mockito.ArgumentCaptor[ServletRequest] = ArgumentCaptor.forClass(classOf[ServletRequest])
        Mockito.verify(filterChain).doFilter(servletRequest.capture(), any())
        val chainRequest = servletRequest.getValue
        val wrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(chainRequest.asInstanceOf[HttpServletRequest])
        wrapper.getHeaderNames exists (_.equalsIgnoreCase("content-type")) shouldBe true
        wrapper.getHeader("content-type") shouldBe "application/json"
        IOUtils.toString(chainRequest.getInputStream) shouldBe bodyContent
      }
    }
    val notReturnParams = List(
      ("there is no body", ""),
      ("there is only white space in the first 8 bytes", "\f \n \r \t this wont get read..."),
      ("there is only white space in the 4 byte input", "    ")
    )
    notReturnParams.foreach { case (testName: String, bodyContent: String) =>
      it(s"should not return the content-type header if $testName") {
        val request: MockHttpServletRequest = new MockHttpServletRequest()
        request.addHeader("content-type", "application/json")
        request.setContent(bodyContent.getBytes)
        val filterChain: FilterChain = mock[FilterChain]

        (new ContentTypeStripperFilter).doFilter(request, mock[ServletResponse], filterChain)

        val servletRequest: org.mockito.ArgumentCaptor[ServletRequest] = ArgumentCaptor.forClass(classOf[ServletRequest])
        Mockito.verify(filterChain).doFilter(servletRequest.capture(), any())
        val chainRequest = servletRequest.getValue
        val wrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(chainRequest.asInstanceOf[HttpServletRequest])
        wrapper.getHeaderNames exists (_.equalsIgnoreCase("content-type")) shouldBe false
        wrapper.getHeader("content-type") shouldBe null
        IOUtils.toString(chainRequest.getInputStream) shouldBe bodyContent
      }
    }
  }
}
