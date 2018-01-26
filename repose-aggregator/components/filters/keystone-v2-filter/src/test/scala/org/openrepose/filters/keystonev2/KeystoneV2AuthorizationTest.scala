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
import org.openrepose.filters.keystonev2.config.{RolesList, ServiceEndpointType, ValidateTenantType}
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

  describe("getRequestTenant") {
    val tenantHeaderName = "X-Tenant-Id-Header"

    it("should return a tenant from the configured tenant header") {
      val tenantId = "someTenant"
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName)
      request.addHeader(tenantHeaderName, tenantId)

      getRequestTenant(config, new HttpServletRequestWrapper(request)) shouldEqual tenantId
    }

    it("should return the highest quality tenant from the configured tenant header without a quality") {
      val tenantIds = Seq("lowTenant;q=0.1",  "midTenant;q=0.5", "bestTenant;q=1.0")
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName)
      tenantIds.foreach(request.addHeader(tenantHeaderName, _))

      getRequestTenant(config, new HttpServletRequestWrapper(request)) shouldEqual "bestTenant"
    }

    it("should return the highest quality tenant from the first configured tenant header") {
      val xTenantIds = Seq("xTenant1;q=0.1",  "xTenant2;q=0.5", "xTenant3;q=1.0")
      val myTenantIds = Seq("myTenant1", "myTenant2")
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName)
      xTenantIds.foreach(request.addHeader(tenantHeaderName, _))
      myTenantIds.foreach(request.addHeader("My-Tenant-Ids", _))

      getRequestTenant(config, new HttpServletRequestWrapper(request)) shouldEqual "xTenant3"
    }

    it("should return a tenant from the URI") {
      val tenantId = "someTenant"
      val request = new MockHttpServletRequest("GET", s"/$tenantId")
      val config = new ValidateTenantType()
          .withUriExtractionRegex("[^/]*/([^/]+)")

      getRequestTenant(config, new HttpServletRequestWrapper(request)) shouldEqual tenantId
    }

    it("should prefer a tenant from the configured tenant header over the URI") {
      val headerTenantId = "headerTenant"
      val uriTenantId = "uriTenant"
      val request = new MockHttpServletRequest("GET", s"/$uriTenantId")
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName)
        .withUriExtractionRegex("[^/]*/([^/]+)")
      request.addHeader(tenantHeaderName, headerTenantId)

      getRequestTenant(config, new HttpServletRequestWrapper(request)) shouldEqual headerTenantId
    }

    it("should throw an exception if no tenant can be found") {
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName)
        .withUriExtractionRegex("[^/]*/([^/]+)")

      an[UnparsableTenantException] should be thrownBy getRequestTenant(config, new HttpServletRequestWrapper(request))
    }
  }

  describe("getTenantScopedRoles") {
    val tenantToValidate = "123456"
    val listOfRoles = List(Role("foo", None), Role("bar", Option("123456")), Role("baz", Option("789012")))

    it("should remove unrelated roles when not in legacy mode") {
      val config = new ValidateTenantType
      config.setEnableLegacyRolesMode(false)

      getTenantScopedRoles(config, tenantToValidate, listOfRoles) should contain only (Role("foo", None), Role("bar", Option("123456")))
    }

    it("shouldn't remove unrelated roles when in legacy mode") {
      val config = new ValidateTenantType
      config.setEnableLegacyRolesMode(true)

      getTenantScopedRoles(config, tenantToValidate, listOfRoles) should contain theSameElementsAs listOfRoles
    }

    it("shouldn't remove any roles when unconfigured") {
      getTenantScopedRoles(null, tenantToValidate, listOfRoles) should contain theSameElementsAs listOfRoles
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

    it("should return None when a check shouldn't be done") {
      getMatchingTenant(config, "123456", false, token) shouldBe None
    }

    it("should return the tenant when it's the default") {
      getMatchingTenant(config, "123456", true, token) shouldBe Some("123456")
    }

    it("should return the tenant when it's in the list") {
      getMatchingTenant(config, "456789", true, token) shouldBe Some("456789")
    }

    it("should return the tenant when it's prefixed and the default") {
      val validToken = ValidToken("", "", Seq.empty, None, None, Some("baz:098765"), Seq("456789","foo:789012", "bar:012345"), None, None, Seq.empty, None, None, None)
      getMatchingTenant(config, "098765", true, validToken) shouldBe Some("baz:098765")
    }

    it("should return the tenant when it's prefixed and in the list") {
      getMatchingTenant(config, "789012", true, token) shouldBe Some("foo:789012")
    }

    it("should return none when the tenant matches but has the wrong prefix") {
      getMatchingTenant(config, "012345", true, token) shouldBe None
    }

    it("should return None when the tenant isn't present") {
      getMatchingTenant(config, "654321", true, token) shouldBe None
    }
  }

  describe("authorizeTenant") {
    it("should succeed when a tenant check is unneeded") {
      authorizeTenant(false, Some("thing")).success
    }

    it("should succeed when there was a tenant match") {
      authorizeTenant(true, Some("thing")).success
    }

    it("should fail when there wasn't a tenant match") {
      authorizeTenant(true, None).failed.get shouldBe a [InvalidTenantException]
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

      it("should fail is the endpoint doesn't match"){
        val endpoints = EndpointsData("", Vector(Endpoint(Option("dfw"), Option("banana"), Option("foo"), "http://foo.com")))
        authorizeEndpoints(config, false, Success(endpoints)).failed.get shouldBe a [UnauthorizedEndpointException]
      }
    }
  }
}
