package org.openrepose.filters.contenttypestripper

import java.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, FunSpec}
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class PushBackHttpServletRequestWrapperTest extends FunSpec with Matchers{
  describe("The PushBackHttpServletRequestWrapper") {
    val mockRequest: MockHttpServletRequest = new MockHttpServletRequest()
    mockRequest.addHeader("Content-Type", "application/foo")
    mockRequest.addHeader("X-Auth", "123456")

    it("should get all the header names on the request") {
      val wrapper = new PushBackHttpServletRequestWrapper(mockRequest)
      val headerNames: util.Enumeration[String] = wrapper.getHeaderNames
      List("Content-Type", "X-Auth").map(_ shouldBe headerNames.nextElement)
      intercept[NoSuchElementException] {
        headerNames.nextElement
      }
    }
    it("should get all the header names but ones added to the blacklist") {
      val wrapper = new PushBackHttpServletRequestWrapper(mockRequest)
      wrapper.addToHeaderBlacklist("Content-Type")
      val headerNames: util.Enumeration[String] = wrapper.getHeaderNames
      headerNames.nextElement shouldBe "X-Auth"
      intercept[NoSuchElementException] {
        headerNames.nextElement
      }
    }
    it("should be able to get a header by name") {
      val wrapper = new PushBackHttpServletRequestWrapper(mockRequest)
      wrapper.getHeader("Content-Type") shouldBe "application/foo"
      wrapper.getHeader("X-Auth") shouldBe "123456"
    }
    it("should return nothing when a header is asked for that is on the blacklist") {
      val wrapper = new PushBackHttpServletRequestWrapper(mockRequest)
      wrapper.addToHeaderBlacklist("Content-Type")
      wrapper.getHeader("X-Auth") shouldBe "123456"
      wrapper.getHeader("Content-Type") shouldBe null
    }
    it("should be case insensitive when checking a header on the blacklist") {
      val wrapper = new PushBackHttpServletRequestWrapper(mockRequest)
      wrapper.addToHeaderBlacklist("content-TyPe")
      wrapper.getHeader("X-Auth") shouldBe "123456"
      wrapper.getHeader("Content-Type") shouldBe null
    }
  }
}
