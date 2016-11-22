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

package org.openrepose.filters.rackspaceauthuser

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletResponse

import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class RackspaceAuthUserFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var filter: RackspaceAuthUserFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: HttpServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = mock[HttpServletResponse]
    filterChain = mock[FilterChain]

    filter = new RackspaceAuthUserFilter(null)
  }

  describe("do filter") {
    it("will return a 500 if the filter is not initialized") {
      filter.doFilter(servletRequest, servletResponse, filterChain)

      verify(servletResponse).sendError(500, "Filter not initialized")
    }

    List("OPTIONS", "GET", "HEAD", "PUT", "DELETE", "TRACE", "CONNECT", "CUSTOM") foreach { httpMethod =>
      it(s"will not update the request for method $httpMethod") {
        filter.configurationUpdated(mock[RackspaceAuthUserConfig])
        servletRequest.setMethod(httpMethod)

        filter.doFilter(servletRequest, servletResponse, filterChain)

        // verify the request was not wrapped and thus not updated
        verify(filterChain).doFilter(servletRequest, servletResponse)
      }
    }

    it("will wrap the request when it's a POST") {
      filter.configurationUpdated(mock[RackspaceAuthUserConfig])
      servletRequest.setMethod("POST")

      filter.doFilter(servletRequest, servletResponse, filterChain)

      // verify the request was wrapped
      verify(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), any(classOf[HttpServletResponse]))
    }
  }
}
