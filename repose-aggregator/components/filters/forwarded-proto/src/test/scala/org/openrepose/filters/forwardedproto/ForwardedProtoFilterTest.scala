package org.openrepose.filters.forwardedproto

import javax.servlet.http.HttpServletRequest
import javax.servlet.{FilterChain, ServletResponse}

import com.mockrunner.mock.web.MockHttpServletRequest
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.collection.JavaConverters._

class ForwardedProtoFilterTest extends FunSpec with Matchers with MockitoSugar {

  val forwardedProtoFilter = new ForwardedProtoFilter()

  describe("doFilter") {
    it("a normal HTTP request should contain X-Forwarded-Proto header with value HTTP/1.1") {
      // given:
      val mockFilterChain = mock[FilterChain]
      val mockRequest = new MockHttpServletRequest()

      // when:
      forwardedProtoFilter.doFilter(mockRequest, null, mockFilterChain)

      // then:
      val servletRequestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(servletRequestCaptor.capture(), any(classOf[ServletResponse]))

      val capturedServletRequest = servletRequestCaptor.getValue
      capturedServletRequest.getHeaders("X-Forwarded-Proto").asScala.size shouldEqual 1
      capturedServletRequest.getHeader("X-Forwarded-Proto") shouldBe "HTTP/1.1"
    }

    it("an https request should contain X-Forwarded-Proto header with value HTTPS") {
      // given:
      val mockFilterChain = mock[FilterChain]
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setProtocol("HTTPS")

      // when:
      forwardedProtoFilter.doFilter(mockRequest, null, mockFilterChain)

      // then:
      val servletRequestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(servletRequestCaptor.capture(), any(classOf[ServletResponse]))

      val capturedServletRequest = servletRequestCaptor.getValue
      capturedServletRequest.getHeaders("X-Forwarded-Proto").asScala.size shouldEqual 1
      capturedServletRequest.getHeader("X-Forwarded-Proto") shouldBe "HTTPS"
    }
  }
}
