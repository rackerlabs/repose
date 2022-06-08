/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.forwardedproto

import javax.servlet.http.HttpServletRequest
import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ForwardedProtoFilterTest extends FunSpec with Matchers with MockitoSugar {

  val forwardedProtoFilter = new ForwardedProtoFilter()

  describe("doFilter") {
    it("a normal HTTP request should contain X-Forwarded-Proto header with value HTTP/1.1") {
      // given:
      val mockFilterChain = mock[FilterChain]
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setProtocol("HTTP/1.1")

      // when:
      forwardedProtoFilter.doFilter(mockRequest, null, mockFilterChain)

      // then:
      val servletRequestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(mockFilterChain).doFilter(servletRequestCaptor.capture(), any(classOf[ServletResponse]))

      val capturedServletRequest = servletRequestCaptor.getValue
      capturedServletRequest.getHeaders("X-Forwarded-Proto").asScala.size shouldEqual 1
      capturedServletRequest.getHeader("X-Forwarded-Proto") shouldBe "HTTP"
    }

    it("an https request should contain X-Forwarded-Proto header with value HTTPS") {
      // given:
      val mockFilterChain = mock[FilterChain]
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setProtocol("HTTPS/1.1")

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
