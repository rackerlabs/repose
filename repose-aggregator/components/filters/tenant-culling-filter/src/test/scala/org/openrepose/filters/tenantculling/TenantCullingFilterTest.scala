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
package org.openrepose.filters.tenantculling

import java.util.Base64
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR

import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.OpenStackServiceHeader.{TENANT_ID, TENANT_ROLES_MAP}
import org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES
import org.openrepose.commons.utils.json.JsonHeaderHelper
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.tenantculling.TenantCullingFilter.TenantToRolesMap
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class TenantCullingFilterTest extends FunSpec with Matchers with MockitoSugar {

  def filter = new TenantCullingFilter()

  describe("doFilter") {
    it("should cull correctly when there is one tenant and one role that match") {
      val request = new MockHttpServletRequest()
      addheaders(request)
      request.addHeader(RELEVANT_ROLES, "foo,banana")
      val filterChain = new MockFilterChain()

      filter.doFilter(request, mock[HttpServletResponse], filterChain)
      val forwardedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]
      val resultMap = JsonHeaderHelper.jsonHeaderToValue(forwardedRequest.getHeader(TENANT_ROLES_MAP)).as[TenantToRolesMap]

      resultMap should contain only ("123456" -> Set("foo", "bar", "baz"))
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain only "123456"
    }

    it("should cull correctly when there is one tenant and multiple roles that match") {
      val request = new MockHttpServletRequest()
      addheaders(request)
      request.addHeader(RELEVANT_ROLES, "foo,banana,bar")
      val filterChain = new MockFilterChain()

      filter.doFilter(request, mock[HttpServletResponse], filterChain)
      val forwardedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]
      val resultMap = JsonHeaderHelper.jsonHeaderToValue(forwardedRequest.getHeader(TENANT_ROLES_MAP)).as[TenantToRolesMap]

      resultMap should contain only ("123456" -> Set("foo", "bar", "baz"))
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain only "123456"
    }

    it("should cull correctly when there are multiple tenants and one role that match") {
      val request = new MockHttpServletRequest()
      addheaders(request)
      request.addHeader(RELEVANT_ROLES, "banana,baz")
      val filterChain = new MockFilterChain()

      filter.doFilter(request, mock[HttpServletResponse], filterChain)
      val forwardedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]
      val resultMap = JsonHeaderHelper.jsonHeaderToValue(forwardedRequest.getHeader(TENANT_ROLES_MAP)).as[TenantToRolesMap]

      resultMap should contain ("123456" -> Set("foo", "bar", "baz"))
      resultMap should contain ("456789" -> Set("baz"))
      resultMap should not contain ("789012" -> Set("wizard"))
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain ("123456")
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain ("456789")
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should not contain "789012"
    }

    it("should cull correctly when there are multiple tenants and multiple roles that match") {
      val request = new MockHttpServletRequest()
      addheaders(request)
      request.addHeader(RELEVANT_ROLES, "foo,wizard")
      val filterChain = new MockFilterChain()

      filter.doFilter(request, mock[HttpServletResponse], filterChain)
      val forwardedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]
      val resultMap = JsonHeaderHelper.jsonHeaderToValue(forwardedRequest.getHeader(TENANT_ROLES_MAP)).as[TenantToRolesMap]

      resultMap should contain ("123456" -> Set("foo", "bar", "baz"))
      resultMap should not contain ("456789" -> Set("baz"))
      resultMap should contain ("789012" -> Set("wizard"))
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain ("123456")
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should not contain "456789"
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain ("789012")
    }

    it("should cull correctly when there are multiple tenants and no roles that match") {
      val request = new MockHttpServletRequest()
      addheaders(request)
      request.addHeader(RELEVANT_ROLES, "banana,shazbot")
      val filterChain = new MockFilterChain()

      filter.doFilter(request, mock[HttpServletResponse], filterChain)
      val forwardedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]
      val resultMap = JsonHeaderHelper.jsonHeaderToValue(forwardedRequest.getHeader(TENANT_ROLES_MAP)).as[TenantToRolesMap]

      resultMap shouldBe empty
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) shouldBe empty
    }

    it("should cull correctly when the relevant roles are qualified") {
      val request = new MockHttpServletRequest()
      addheaders(request)
      request.addHeader(RELEVANT_ROLES, "bar/{tenant1}, banana/{tenant2}")
      val filterChain = new MockFilterChain()

      filter.doFilter(request, mock[HttpServletResponse], filterChain)
      val forwardedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequestWrapper]
      val resultMap = JsonHeaderHelper.jsonHeaderToValue(forwardedRequest.getHeader(TENANT_ROLES_MAP)).as[TenantToRolesMap]

      resultMap should contain only ("123456" -> Set("foo", "bar", "baz"))
      forwardedRequest.getSplittableHeaderScala(TENANT_ID) should contain only "123456"
    }

    it("should fail if the role map header is missing") {
      val request = new MockHttpServletRequest()
      request.addHeader(RELEVANT_ROLES, "banana")
      val response = new MockHttpServletResponse

      filter.doFilter(request, response, mock[FilterChain])

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("should fail is the role map isn't properly encoded") {
      val request = new MockHttpServletRequest()
      request.addHeader(TENANT_ROLES_MAP, "(╯°□°）╯︵ ┻━┻")
      val response = new MockHttpServletResponse

      filter.doFilter(request, response, mock[FilterChain])

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("should fail is the role map isn't properly formatted json") {
      val request = new MockHttpServletRequest()
      request.addHeader(TENANT_ROLES_MAP, Base64.getEncoder.encodeToString(""" {"bananaphone" : "poo" """.getBytes))
      val response = new MockHttpServletResponse

      filter.doFilter(request, response, mock[FilterChain])

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }
  }

  def addheaders(request: MockHttpServletRequest): Unit = {
    val tenants = "123456,456789,789012"
    val tenantToRolesMap = Map("repose/domain/roles" -> Set("dragon"),
                               "123456" -> Set("foo", "bar", "baz"),
                               "456789" -> Set("baz"),
                               "789012" -> Set("wizard"))
    request.addHeader(TENANT_ID, tenants)
    request.addHeader(TENANT_ROLES_MAP, JsonHeaderHelper.anyToJsonHeader(tenantToRolesMap))
  }
}
