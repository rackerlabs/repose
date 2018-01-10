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

import javax.servlet.http.HttpServletResponse.{SC_FORBIDDEN, SC_UNAUTHORIZED}

import org.junit.runner.RunWith
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.Reject
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.{InvalidTenantException, UnauthorizedEndpointException, UnparseableTenantException}
import org.openrepose.filters.keystonev2.KeystoneV2AuthorizationFilter.{InvalidTokenException, MissingTokenException}
import org.openrepose.filters.keystonev2.KeystoneV2Common.TokenRequestAttributeName
import org.openrepose.filters.keystonev2.KeystoneV2TestCommon.createValidToken
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
  }

  describe("doAuth") {
    it("should ...") {
      pending
    }
  }
}
