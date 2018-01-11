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
package org.openrepose.filters.keystonev2

import javax.servlet.http.HttpServletResponse.{SC_FORBIDDEN, SC_INTERNAL_SERVER_ERROR, SC_UNAUTHORIZED}

import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.Reject
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.{InvalidTenantException, UnauthorizedEndpointException, UnparseableTenantException}
import org.openrepose.filters.keystonev2.KeystoneV2AuthorizationFilter.{InvalidEndpointsException, InvalidTokenException, MissingEndpointsException, MissingTokenException}
import org.openrepose.filters.keystonev2.KeystoneV2Common.{EndpointsData, EndpointsRequestAttributeName, TokenRequestAttributeName}
import org.openrepose.filters.keystonev2.KeystoneV2TestCommon.createValidToken
import org.openrepose.filters.keystonev2.config.TenantHandlingType.SendTenantIdQuality
import org.openrepose.filters.keystonev2.config.{KeystoneV2Config, TenantHandlingType, ValidateTenantType}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class KeystoneV2AuthorizationFilterTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var keystoneV2AuthorizationFilter: KeystoneV2AuthorizationFilter = _
  var mockConfigService: ConfigurationService = _

  override def beforeEach(): Unit = {
    mockConfigService = mock[ConfigurationService]
    keystoneV2AuthorizationFilter = new KeystoneV2AuthorizationFilter(mockConfigService)
  }

  describe("getToken") {
    it(s"should return a token if a valid token is present at the $TokenRequestAttributeName attribute of the request") {
      val token = createValidToken()
      val request = new MockHttpServletRequest
      request.setAttribute(TokenRequestAttributeName, token)

      val result = keystoneV2AuthorizationFilter.getToken(request)

      result shouldBe a[Success[_]]
      result.get shouldBe token
    }

    it(s"should return a Failure if a token is absent at the $TokenRequestAttributeName attribute of the request") {
      val request = new MockHttpServletRequest

      val result = keystoneV2AuthorizationFilter.getToken(request)

      result shouldBe a[Failure[_]]
      a[MissingTokenException] should be thrownBy result.get
    }

    it(s"should return a Failure if the object present at the $TokenRequestAttributeName attribute of the request is not a valid token") {
      val token = "not-a-token"
      val request = new MockHttpServletRequest
      request.setAttribute(TokenRequestAttributeName, token)

      val result = keystoneV2AuthorizationFilter.getToken(request)

      result shouldBe a[Failure[_]]
      an[InvalidTokenException] should be thrownBy result.get
    }
  }

  describe("getEndpoints") {
    it(s"should return an endpoints object if valid endpoints are present at the $EndpointsRequestAttributeName attribute of the request") {
      val endpoints = EndpointsData("", Vector.empty)
      val request = new MockHttpServletRequest
      request.setAttribute(EndpointsRequestAttributeName, endpoints)

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result shouldBe a[Success[_]]
      result.get shouldBe endpoints
    }

    it(s"should return a Failure if endpoints are absent at the $EndpointsRequestAttributeName attribute of the request") {
      val request = new MockHttpServletRequest

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result shouldBe a[Failure[_]]
      a[MissingEndpointsException] should be thrownBy result.get
    }

    it(s"should return a Failure if the object present at the $EndpointsRequestAttributeName attribute of the request is not a valid endpoints object") {
      val endpoints = "not-endpoints"
      val request = new MockHttpServletRequest
      request.setAttribute(EndpointsRequestAttributeName, endpoints)

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result shouldBe a[Failure[_]]
      an[InvalidEndpointsException] should be thrownBy result.get
    }
  }

  describe("scopeTenantIdHeader") {
    it("should add the matching tenant with the configured quality") {
      val sendAllTenantIds = true
      val matchedTenantQuality = 0.66
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
          .withSendTenantIdQuality(new SendTenantIdQuality()
            .withUriTenantQuality(matchedTenantQuality)
          )
        )

      val tenantIds = Seq("tenant1", "tenant2")
      val matchedTenantId = "matchedTenant"
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.addHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantId)

      request.getHeadersScala(TENANT_ID) should contain only (tenantIds :+ s"$matchedTenantId;q=$matchedTenantQuality": _*)
    }

    it("should add the matching tenant with no quality") {
      val sendAllTenantIds = true
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantId = "matchedTenant"
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.addHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantId)

      request.getHeadersScala(TENANT_ID) should contain only (tenantIds :+ matchedTenantId: _*)
    }

    it("should replace existing tenants with the matching tenant with the configured quality") {
      val sendAllTenantIds = false
      val matchedTenantQuality = 0.66
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
          .withSendTenantIdQuality(new SendTenantIdQuality()
            .withUriTenantQuality(matchedTenantQuality)
          )
        )

      val tenantIds = Seq("tenant1", "tenant2")
      val matchedTenantId = "matchedTenant"
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.addHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantId)

      request.getHeadersScala(TENANT_ID) should contain only s"$matchedTenantId;q=$matchedTenantQuality"
    }

    it("should replace existing tenants with the matching tenant with no quality") {
      val sendAllTenantIds = false
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantId = "matchedTenant"
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.addHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantId)

      request.getHeadersScala(TENANT_ID) should contain only matchedTenantId
    }
  }

  describe("handleFailures") {
    it("should reject as unauthorized when a failure is caused by an invalid tenant") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(InvalidTenantException("Invalid tenant")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_UNAUTHORIZED
    }

    it("should reject as forbidden when a failure is caused by an unauthorized endpoint") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(UnauthorizedEndpointException("Unauthorized endpoint")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_FORBIDDEN
    }

    it("should reject as unauthorized when a failure is caused by an unparseable tenant") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(UnparseableTenantException("Unparseable tenant")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_UNAUTHORIZED
    }

    it("should reject as an internal server error when a failure is caused by a missing token") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(MissingTokenException("Missing token")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_INTERNAL_SERVER_ERROR
    }

    it("should reject as an internal server error when a failure is caused by a missing endpoints object") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(MissingEndpointsException("Missing endpoints")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_INTERNAL_SERVER_ERROR
    }

    it("should reject as an internal server error when a failure is caused by an invalid token") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(InvalidTokenException("Invalid token")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_INTERNAL_SERVER_ERROR
    }

    it("should reject as an internal server error when a failure is caused by an invalid endpoints object") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(InvalidEndpointsException("Invalid endpoints")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_INTERNAL_SERVER_ERROR
    }
  }

  describe("doAuth") {
    it("should pass a request with scoped tenants") {
      val inputTenantHeaderName = "My-Tenant"
      val userTenantId = "user1"
      val miscTenantIds = Seq("hammer2", "bolt3")
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withValidateTenant(new ValidateTenantType()
            .withUriExtractionRegex("[^/]*/([^/]+)")))

      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      request.setAttribute(TokenRequestAttributeName, createValidToken(defaultTenantId = Some(userTenantId), tenantIds = miscTenantIds))
      request.setRequestURI(s"/$userTenantId")
      request.addHeader(inputTenantHeaderName, userTenantId)
      miscTenantIds.foreach(request.addHeader(TENANT_ID, _))

      val result = keystoneV2AuthorizationFilter.doAuth(request)

      result shouldBe a[Success[_]]
      request.getHeadersScala(TENANT_ID) should contain only userTenantId
    }
  }
}
