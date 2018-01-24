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
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.Reject
import org.openrepose.filters.keystonev2.KeystoneV2Common.{Endpoint, EndpointsData, Role, ValidToken}
import org.openrepose.filters.keystonev2.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, PartialFunctionValues, TryValues}
import org.springframework.mock.web.MockHttpServletRequest

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class KeystoneV2AuthorizationTest  extends FunSpec
  with org.scalatest.Matchers
  with BeforeAndAfterEach
  with PartialFunctionValues
  with TryValues {

  import KeystoneV2Authorization._

  describe("handleFailures") {
    List((InvalidTenantException("Foo"), SC_UNAUTHORIZED, "Foo"),
         (UnauthorizedEndpointException("Bar"), SC_FORBIDDEN, "Bar"),
         (UnparsableTenantException("Baz"), SC_UNAUTHORIZED, "Baz")).foreach { case(exception, statusCode, message) =>

      it(s"should return $statusCode for ${exception.getClass.getSimpleName}") {
        handleFailures.valueAt(Failure(exception)) should matchPattern { case Reject(status, Some(responseMessage), _) if (status == statusCode) && (responseMessage == message) => }
      }
    }
  }

  describe("getRequestTenants") {
    val tenantHeaderName = "X-Tenant-Id-Header"

    it("should return a tenant from the configured tenant header") {
      val tenantId = "someTenant"
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withUriExtractionRegexAndHeaderExtractionName(
          new HeaderExtractionType().withValue(tenantHeaderName))
      request.addHeader(tenantHeaderName, tenantId)

      getRequestTenants(config, new HttpServletRequestWrapper(request)) should contain only tenantId
    }

    it("should return a tenant from the URI") {
      val tenantId = "someTenant"
      val request = new MockHttpServletRequest("GET", s"/$tenantId")
      val config = new ValidateTenantType()
        .withUriExtractionRegexAndHeaderExtractionName(
          new UriExtractionType().withValue("[^/]*/([^/]+)"))

      getRequestTenants(config, new HttpServletRequestWrapper(request)) should contain only tenantId
    }

    it("should return all tenants from the URI and configured tenant header") {
      val headerTenantId = "headerTenant"
      val uriTenantId = "uriTenant"
      val request = new MockHttpServletRequest("GET", s"/$uriTenantId")
      val config = new ValidateTenantType()
        .withUriExtractionRegexAndHeaderExtractionName(
          new HeaderExtractionType().withValue(tenantHeaderName),
          new UriExtractionType().withValue("[^/]*/([^/]+)"))
      request.addHeader(tenantHeaderName, headerTenantId)

      getRequestTenants(config, new HttpServletRequestWrapper(request)) should contain only (headerTenantId, uriTenantId)
    }

    it("should throw an exception if no tenant can be found") {
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withUriExtractionRegexAndHeaderExtractionName(
          new HeaderExtractionType().withValue(tenantHeaderName),
          new UriExtractionType().withValue("[^/]*/([^/]+)"))

      an[UnparsableTenantException] should be thrownBy getRequestTenants(config, new HttpServletRequestWrapper(request))
    }
  }

  describe("getTenantScopedRoles") {
    val tenantsToValidate = Set("123456", "654321")
    val listOfRoles = List(Role("foo", None), Role("bar", Option("123456")), Role("baz", Option("789012")), Role("qux", Option("654321")))

    it("should remove unrelated roles when not in legacy mode") {
      val config = new ValidateTenantType
      config.setEnableLegacyRolesMode(false)

      getTenantScopedRoles(config, tenantsToValidate, listOfRoles) should contain only (Role("foo", None), Role("bar", Option("123456")), Role("qux", Option("654321")))
    }

    it("shouldn't remove unrelated roles when in legacy mode") {
      val config = new ValidateTenantType
      config.setEnableLegacyRolesMode(true)

      getTenantScopedRoles(config, tenantsToValidate, listOfRoles) should contain theSameElementsAs listOfRoles
    }

    it("shouldn't remove any roles when unconfigured") {
      getTenantScopedRoles(null, tenantsToValidate, listOfRoles) should contain theSameElementsAs listOfRoles
    }
  }

  describe("isUserPreAuthed") {
    val listOfRoles = List(Role("foo", None), Role("bar", Option("123456")), Role("baz", Option("789012")))

    it("should be true when the role is present in the list") {
      val config = new RolesList().withRole("bar")

      isUserPreAuthed(config, listOfRoles) shouldBe true
    }

    it("should be false when the role is absent from the list") {
      val config = new RolesList().withRole("banana")

      isUserPreAuthed(config, listOfRoles) shouldBe false
    }

    it("should be false when there is no configuration") {
      isUserPreAuthed(null, listOfRoles) shouldBe false
    }
  }

  describe("shouldAuthorizeTenant") {
    it("should return false when unconfigured") {
      shouldAuthorizeTenant(null, false) shouldBe false
    }

    it("should return false when pre-authorized") {
      shouldAuthorizeTenant(new ValidateTenantType, true) shouldBe false
    }

    it("should return true when not pre-authorized") {
      shouldAuthorizeTenant(new ValidateTenantType, false) shouldBe true
    }
  }

  describe("getMatchingTenant") {
    val token = ValidToken("", "", Seq.empty, None, None, Some("123456"), Seq("456789","foo:789012", "bar:012345"), None, None, Seq.empty, None, None, None)
    val config = new ValidateTenantType().withStripTokenTenantPrefixes("foo:/baz:")

    it("should return an empty collection when a check shouldn't be done") {
      getMatchingTenants(config, Set("123456"), false, token) shouldBe empty
    }

    it("should return the tenant when it's the default") {
      getMatchingTenants(config, Set("123456"), true, token) should contain only "123456"
    }

    it("should return the tenant when it's in the list") {
      getMatchingTenants(config, Set("456789"), true, token) should contain only "456789"
    }

    it("should return the tenant when it's prefixed and the default") {
      val validToken = ValidToken("", "", Seq.empty, None, None, Some("baz:098765"), Seq("456789", "foo:789012", "bar:012345"), None, None, Seq.empty, None, None, None)
      getMatchingTenants(config, Set("098765"), true, validToken) should contain only "baz:098765"
    }

    it("should return the tenant when it's prefixed and in the list") {
      getMatchingTenants(config, Set("789012"), true, token) should contain only "foo:789012"
    }

    it("should return an empty collection when the tenant matches but has the wrong prefix") {
      getMatchingTenants(config, Set("012345"), true, token) shouldBe empty
    }

    it("should return an empty collection when the tenant isn't present") {
      getMatchingTenants(config, Set("654321"), true, token) shouldBe empty
    }

    it("should return multiple matching tenants") {
      getMatchingTenants(config, Set("123456", "456789"), true, token) should contain only("123456", "456789")
    }
  }

  describe("authorizeTenant") {
    it("should succeed when a tenant check is unneeded") {
      authorizeTenant(false, Set("thing"), Set("thing")).success
    }

    it("should succeed when all tenants had a match") {
      authorizeTenant(true, Set("thing"), Set("thing")).success
    }

    it("should fail when not all tenants had a match") {
      authorizeTenant(true, Set("thing", "other-thing"), Set("thing")).failed.get shouldBe a [InvalidTenantException]
    }

    it("should fail when there wasn't a tenant match") {
      authorizeTenant(true, Set("thing"), Set.empty).failed.get shouldBe a [InvalidTenantException]
    }
  }

  describe("authorizeEndpoints") {
    val config = new ServiceEndpointType().withName("banana").withRegion("ord").withPublicUrl("http://foo.com")

    it("should succeed if unconfigured") {
      authorizeEndpoints(null, false, Success(EndpointsData("", Vector.empty))).success
    }

    it("should succeed if preauthed") {
      authorizeEndpoints(config, true, Success(EndpointsData("", Vector.empty))).success
    }

    it("should succeed if the endpoint matches") {
      authorizeEndpoints(config, false, Success(EndpointsData("", Vector(Endpoint(Option("ord"), Option("banana"), Option("foo"), "http://foo.com"))))).success
    }

    it("should fail if the endpoint doesn't match"){
      val endpoints = EndpointsData("", Vector(Endpoint(Option("dfw"), Option("banana"), Option("foo"), "http://foo.com")))
      authorizeEndpoints(config, false, Success(endpoints)).failed.get shouldBe a [UnauthorizedEndpointException]
    }
  }
}
