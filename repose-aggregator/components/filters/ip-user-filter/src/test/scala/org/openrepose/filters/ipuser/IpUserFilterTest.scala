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

package org.openrepose.filters.ipuser

import javax.servlet.http.HttpServletResponse
import org.junit.runner.RunWith
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.ipuser.config.{GroupType, IpUserConfig}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class IpUserFilterTest extends FunSpec with BeforeAndAfterEach with Matchers {

  val DefaultQuality = ";q=0.4"

  var ipUserFilter: IpUserFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _

  override def beforeEach() = {
    ipUserFilter = new IpUserFilter(null)
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain

    ipUserFilter.configurationUpdated(createConfig())
  }

  describe("setting the request x-pp-user header") {
    it("should use the value of the remote IP address of the request when no X-Forwarded-For header is included") {
      servletRequest.setRemoteAddr("10.1.2.3")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-user") shouldEqual "10.1.2.3" + DefaultQuality
    }

    it("should use the value of the X-Forwarded-For header when it is included") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-user") shouldEqual "10.55.66.77" + DefaultQuality
    }

    it("should use the first value of the X-Forwarded-For header when there are multiple non-split values") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.123.123.123")
      servletRequest.addHeader("X-Forwarded-For", "10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-user") shouldEqual "10.123.123.123" + DefaultQuality
    }

    it("should use the first value of the X-Forwarded-For header when there are multiple splittable values") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.233.10.67,10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-user") shouldEqual "10.233.10.67" + DefaultQuality
    }
  }

  describe("setting the request x-pp-groups header") {
    it("should use the value of the remote IP address of the request when no X-Forwarded-For header is included and group matches") {
      servletRequest.setRemoteAddr("10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") shouldEqual "my-awesome-group" + DefaultQuality
    }

    it("should use the value of the X-Forwarded-For header when it is included") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") shouldEqual "my-awesome-group" + DefaultQuality
    }

    it("group should be null when there are multiple non-split values in the X-Forwarded-For header - last match") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.1.1.1")
      servletRequest.addHeader("X-Forwarded-For", "10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") should be (null)
    }

    it("group should use the matching ip when there are multiple non-split values in the X-Forwarded-For header - first match") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.55.66.77")
      servletRequest.addHeader("X-Forwarded-For", "10.1.1.1")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") shouldEqual "my-awesome-group" + DefaultQuality
    }

    it("group should be null when there are multiple splittable values in the X-Forwarded-For header - last match") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.1.1.1,10.55.66.77")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") should be (null)
    }

    it("group should use the matching ip when there are multiple splittable values in the X-Forwarded-For header - first match") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.55.66.77,10.1.1.1")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") shouldEqual "my-awesome-group" + DefaultQuality
    }

    it("should use the first matching group when the ip in X-Forwarded-For is in multiple groups") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "123.32.1.1")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") shouldEqual "my-awesome-group" + DefaultQuality
    }

    it("should use the last matching group when the ip in X-Forwarded-For") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "10.123.123.123")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      getPostFilterRequest.getHeader("x-pp-groups") shouldEqual "some-other-group" + DefaultQuality
    }
  }

  describe("when presented with non-sense") {
    it("should return a 400 when X-Forwarded-For is malformed") {
      servletRequest.setRemoteAddr("10.1.2.3")
      servletRequest.addHeader("X-Forwarded-For", "banana-phone")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getStatus shouldBe HttpServletResponse.SC_BAD_REQUEST
    }

    it("should return a 400 when remote Address is malformed") {
      //Ideally this would return a 500, but for this to even happen somebody has to be using their own wrapper,
      // and they have to override the behavior for getRemoteAddr. This case seems unlikely enough to not be worth the
      // complexity it would add to the production code, and we have elected to allow the misbehavior of this use case.
      servletRequest.setRemoteAddr("banana-phone")

      ipUserFilter.doFilter(servletRequest, servletResponse, filterChain)

      servletResponse.getStatus shouldBe HttpServletResponse.SC_BAD_REQUEST
    }
  }

  def getPostFilterRequest: HttpServletRequestWrapper = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]

  def createConfig(): IpUserConfig = {
    val myAwesomeGroup = new GroupType
    myAwesomeGroup.setName("my-awesome-group")
    myAwesomeGroup.getCidrIp.add("10.55.66.77/32")
    myAwesomeGroup.getCidrIp.add("10.55.66.78/32")
    myAwesomeGroup.getCidrIp.add("123.32.1.1/32")

    val someOtherGroup = new GroupType
    someOtherGroup.setName("some-other-group")
    someOtherGroup.getCidrIp.add("10.123.123.123/32")
    someOtherGroup.getCidrIp.add("123.32.1.1/32")

    val config = new IpUserConfig
    config.getGroup.add(myAwesomeGroup)
    config.getGroup.add(someOtherGroup)
    config
  }
}
