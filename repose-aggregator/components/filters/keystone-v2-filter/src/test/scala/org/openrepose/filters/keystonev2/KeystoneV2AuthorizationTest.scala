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
import org.openrepose.filters.keystonev2.KeystoneV2Common._
import org.openrepose.filters.keystonev2.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, PartialFunctionValues, TryValues}
import org.springframework.mock.web.MockHttpServletRequest

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class KeystoneV2AuthorizationTest extends FunSpec
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
        .withHeaderExtractionName(tenantHeaderName)
      request.addHeader(tenantHeaderName, tenantId)

      getRequestTenants(config, new HttpServletRequestWrapper(request)) should contain only tenantId
    }

    it("should return all tenants from the configured tenant headers") {
      val tenantHeaderNameTwo = "My-Tenant-Header"
      val headerTenantIdOne = "headerTenantOne"
      val headerTenantIdTwo = "headerTenantTwo"
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName, tenantHeaderNameTwo)
      request.addHeader(tenantHeaderName, headerTenantIdOne)
      request.addHeader(tenantHeaderNameTwo, headerTenantIdTwo)

      getRequestTenants(config, new HttpServletRequestWrapper(request)) should contain only(headerTenantIdOne, headerTenantIdTwo)
    }

    it("should throw an exception if no tenant can be found") {
      val request = new MockHttpServletRequest()
      val config = new ValidateTenantType()
        .withHeaderExtractionName(tenantHeaderName)

      an[UnparsableTenantException] should be thrownBy getRequestTenants(config, new HttpServletRequestWrapper(request))
    }
  }

  describe("getScopedTenantToRolesMap") {
    val tenantsToValidate = Set("123456", "654321")
    val tenantToRolesMap = Map("123456" -> Set("bar"), "789012" -> Set("baz"), "654321" -> Set("qux"))

    it("should remove unrelated tenants and roles") {
      getScopedTenantToRolesMap(Array.empty, tenantToRolesMap, tenantsToValidate) should contain only("123456" -> Set("bar"), "654321" -> Set("qux"))
    }

    it("should return an empty collection when the no tenant matches") {
      getScopedTenantToRolesMap(Array.empty, tenantToRolesMap, Set("13579")) shouldBe empty
    }

    it("should retain tenant-less (domain-level) roles") {
      val domainTenantToRoles = DomainRoleTenantKey -> Set("97531")

      getScopedTenantToRolesMap(Array.empty, tenantToRolesMap + domainTenantToRoles, tenantsToValidate) should contain(DomainRoleTenantKey -> Set("97531"))
    }

    it("should retain a matching prefixed tenant with associated roles") {
      val prefixedTenantEntry = "buzz:13579" -> Set("97531")

      getScopedTenantToRolesMap(Array("buzz:"), tenantToRolesMap + prefixedTenantEntry, Set("13579")) should contain(prefixedTenantEntry)
    }

    it("should return an empty collection when the tenant matches but has the wrong prefix") {
      val prefixedTenantEntry = "buzz:13579" -> Set("97531")

      getScopedTenantToRolesMap(Array("fizz:"), tenantToRolesMap + prefixedTenantEntry, Set("13579")) shouldBe empty
    }
  }

  describe("isUserPreAuthed") {
    val listOfRoles = List("foo", "bar", "baz")

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

  describe("authorizeTenant") {
    it("should succeed when all tenants had a match") {
      authorizeTenant(Array("foo:"), Set("thing"), Set("thing")).success
    }

    it("should succeed when a prefixed tenant had a match") {
      authorizeTenant(Array("foo:"), Set("foo:thing"), Set("thing")).success
    }

    it("should fail when not all tenants had a match") {
      authorizeTenant(Array("foo:"), Set("thing"), Set("thing", "other-thing")).failed.get shouldBe a [InvalidTenantException]
    }

    it("should fail when there wasn't a tenant match") {
      authorizeTenant(Array("foo:"), Set.empty, Set("thing")).failed.get shouldBe a [InvalidTenantException]
    }
  }

  describe("authorizeEndpoints") {
    val config = new ServiceEndpointType().withName("banana").withRegion("ord").withPublicUrl("http://foo.com")

    it("should succeed if the endpoint matches") {
      authorizeEndpoints(config, Success(EndpointsData("", Vector(Endpoint(Option("ord"), Option("banana"), Option("foo"), "http://foo.com"))))).success
    }

    it("should fail if the endpoint doesn't match"){
      val endpoints = EndpointsData("", Vector(Endpoint(Option("dfw"), Option("banana"), Option("foo"), "http://foo.com")))
      authorizeEndpoints(config, Success(endpoints)).failed.get shouldBe a [UnauthorizedEndpointException]
    }
  }
}
