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
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.util.Failure

@RunWith(classOf[JUnitRunner])
class KeystoneV2AuthorizationFilterTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var keystoneV2AuthorizationFilter: KeystoneV2AuthorizationFilter = _
  var mockConfigService: ConfigurationService = _

  override def beforeEach(): Unit = {
    mockConfigService = mock[ConfigurationService]
    keystoneV2AuthorizationFilter = new KeystoneV2AuthorizationFilter(mockConfigService)
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
