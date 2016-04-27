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

import org.junit.runner.RunWith
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.ipuser.config.{GroupType, IpUserConfig}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class IpUserFilterTest extends FunSpec with BeforeAndAfter with Matchers {

  val DefaultQuality = ";q=0.4"

  var ipUserFilter: IpUserFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _

  before {
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

  def getPostFilterRequest: HttpServletRequestWrapper = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]

  def createConfig(): IpUserConfig = {
    val group = new GroupType
    group.setName("ipv4-match-all")
    group.getCidrIp.add("0.0.0.0/0")

    val config = new IpUserConfig
    config.getGroup.add(group)
    config
  }
}
