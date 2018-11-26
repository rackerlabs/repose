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
import org.openrepose.commons.utils.http.OpenStackServiceHeader.{ROLES, TENANT_ID, TENANT_ROLES_MAP}
import org.openrepose.commons.utils.http.PowerApiHeader.X_CATALOG
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.commons.utils.string.Base64Helper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.Reject
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.{InvalidTenantException, UnauthorizedEndpointException, UnparsableTenantException}
import org.openrepose.filters.keystonev2.KeystoneV2AuthorizationFilter.{InvalidEndpointsException, InvalidTenantToRolesMapException, MissingEndpointsException, MissingTenantToRolesMapException}
import org.openrepose.filters.keystonev2.KeystoneV2Common.Endpoint
import org.openrepose.filters.keystonev2.config.TenantHandlingType.SendTenantIdQuality
import org.openrepose.filters.keystonev2.config.{KeystoneV2Config, TenantHandlingType, ValidateTenantType}
import org.scalatest.TryValues._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, Json, Writes}

import scala.util.Failure

@RunWith(classOf[JUnitRunner])
class KeystoneV2AuthorizationFilterTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var keystoneV2AuthorizationFilter: KeystoneV2AuthorizationFilter = _
  var mockConfigService: ConfigurationService = _

  override def beforeEach(): Unit = {
    mockConfigService = mock[ConfigurationService]
    keystoneV2AuthorizationFilter = new KeystoneV2AuthorizationFilter(mockConfigService)
  }

  describe("getTenantToRolesMap") {
    it(s"should return a tenant to roles map if a valid map is present in the $TENANT_ROLES_MAP header of the request") {
      val tenantToRolesMap = Map("tenant1" -> Set("role1", "role2"), "tenant3" -> Set("role3"))
      val tenantToRolesJson = Json.stringify(Json.toJson(tenantToRolesMap))
      val encodedTenantToRolesJson = Base64Helper.base64EncodeUtf8(tenantToRolesJson)
      val request = new MockHttpServletRequest
      request.addHeader(TENANT_ROLES_MAP, encodedTenantToRolesJson)

      val result = keystoneV2AuthorizationFilter.getTenantToRolesMap(request)

      result.success.get shouldEqual tenantToRolesMap
    }

    it(s"should return a Failure if a token is absent at the $TENANT_ROLES_MAP header of the request") {
      val request = new MockHttpServletRequest

      val result = keystoneV2AuthorizationFilter.getTenantToRolesMap(request)

      result.failure.exception shouldBe a[MissingTenantToRolesMapException]
    }

    it(s"should return a Failure if the object present at the $TENANT_ROLES_MAP header of the request is not a valid token") {
      val tenantToRolesMap = "not-a-map"
      val request = new MockHttpServletRequest
      request.addHeader(TENANT_ROLES_MAP, tenantToRolesMap)

      val result = keystoneV2AuthorizationFilter.getTenantToRolesMap(request)

      result.failure.exception shouldBe an[InvalidTenantToRolesMapException]
    }
  }

  describe("getEndpoints") {
    import KeystoneV2AuthorizationFilterTest.endpointWrites

    it(s"should return an endpoints object if valid endpoints are present in the $X_CATALOG header of the request") {
      val endpoints = Vector(Endpoint(None, None, None, "first"), Endpoint(None, None, None, "second"))
      val catalog = Base64Helper.base64EncodeUtf8(Json.stringify(Json.obj("endpoints" -> Json.toJson(endpoints))))
      val request = new MockHttpServletRequest
      request.addHeader(X_CATALOG, catalog)

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result.success.get.vector should contain theSameElementsAs endpoints
    }

    it(s"should return a Failure if endpoints are absent in the $X_CATALOG header of the request") {
      val request = new MockHttpServletRequest

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result.failure.exception shouldBe a[MissingEndpointsException]
    }

    it(s"should return a Failure if the value present in the $X_CATALOG header of the request is not base 64 encoded") {
      val endpoints = Vector(Endpoint(None, None, None, "first"), Endpoint(None, None, None, "second"))
      val catalog = Json.stringify(Json.obj("endpoints" -> Json.toJson(endpoints))).getBytes
      val request = new MockHttpServletRequest
      request.addHeader(X_CATALOG, catalog)

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result.failure.exception shouldBe an[InvalidEndpointsException]
    }

    it(s"should return a Failure if the value present in the $X_CATALOG header of the request is not a valid endpoints representation") {
      val endpoints = Vector("first", "second")
      val catalog = Base64Helper.base64EncodeUtf8(Json.stringify(Json.obj("endpoints" -> Json.toJson(endpoints))))
      val request = new MockHttpServletRequest
      request.addHeader(X_CATALOG, catalog)

      val result = keystoneV2AuthorizationFilter.getEndpoints(request)

      result.failure.exception shouldBe an[InvalidEndpointsException]
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
            .withValidatedTenantQuality(matchedTenantQuality)
          )
        )

      val tenantIds = Seq("tenant1", "tenant2")
      val matchedTenantIds = Set("matchedTenant")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (tenantIds ++ matchedTenantIds.map(t => s"$t;q=$matchedTenantQuality"): _*)
    }

    it("should add the matching tenant with no quality") {
      val sendAllTenantIds = true
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantIds = Set("matchedTenant")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (tenantIds ++ matchedTenantIds: _*)
    }

    it("should replace existing tenants with the matching tenant with the configured quality") {
      val sendAllTenantIds = false
      val matchedTenantQuality = 0.66
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
          .withSendTenantIdQuality(new SendTenantIdQuality()
            .withValidatedTenantQuality(matchedTenantQuality)
          )
        )

      val tenantIds = Seq("tenant1", "tenant2")
      val matchedTenantIds = Set("matchedTenant")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (matchedTenantIds.map(t => s"$t;q=$matchedTenantQuality").toSeq: _*)
    }

    it("should replace existing tenants with the matching tenant with no quality") {
      val sendAllTenantIds = false
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantIds = Set("matchedTenant")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (matchedTenantIds.toSeq: _*)
    }

    it("should append multiple matching tenants with the configured quality") {
      val sendAllTenantIds = true
      val matchedTenantQuality = 0.66
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
          .withSendTenantIdQuality(new SendTenantIdQuality()
            .withValidatedTenantQuality(matchedTenantQuality)
          )
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantIds = Set("matchedTenant1", "matchedTenant2")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (tenantIds ++ matchedTenantIds.map(t => s"$t;q=$matchedTenantQuality"): _*)
    }

    it("should append multiple matching tenants with no quality") {
      val sendAllTenantIds = true
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantIds = Set("matchedTenant1", "matchedTenant2")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (tenantIds ++ matchedTenantIds: _*)
    }

    it("should replace existing tenants with multiple matching tenants with the configured quality") {
      val sendAllTenantIds = false
      val matchedTenantQuality = 0.66
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
          .withSendTenantIdQuality(new SendTenantIdQuality()
            .withValidatedTenantQuality(matchedTenantQuality)
          )
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantIds = Set("matchedTenant1", "matchedTenant2")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (matchedTenantIds.map(t => s"$t;q=$matchedTenantQuality").toSeq: _*)
    }

    it("should replace existing tenants with multiple matching tenants with no quality") {
      val sendAllTenantIds = false
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds)
        )

      val tenantIds = Seq("tenant1;q=0.2", "tenant2;q=0.4")
      val matchedTenantIds = Set("matchedTenant1", "matchedTenant2")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      tenantIds.foreach(request.appendHeader(TENANT_ID, _))

      keystoneV2AuthorizationFilter.scopeTenantIdHeader(request, matchedTenantIds)

      request.getSplittableHeaderScala(TENANT_ID) should contain only (matchedTenantIds.toSeq: _*)
    }
  }

  describe("scopeRolesHeader") {
    it("should do nothing if in legacy roles mode") {
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withValidateTenant(new ValidateTenantType()
            .withEnableLegacyRolesMode(true)))

      val requestRoles = Set("reqone", "reqtwo")
      val tokenRoles = Set("one", "two")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      requestRoles.foreach(request.appendHeader(ROLES, _))

      keystoneV2AuthorizationFilter.scopeRolesHeader(request, tokenRoles)

      request.getSplittableHeaderScala(ROLES) should contain only (requestRoles.toSeq: _*)
    }

    it(s"should set the $ROLES header to the provided roles") {
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType())

      val tokenRoles = Set("one", "two")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)

      keystoneV2AuthorizationFilter.scopeRolesHeader(request, tokenRoles)

      request.getSplittableHeaderScala(ROLES) should contain only (tokenRoles.toSeq: _*)
    }

    it(s"should replace existing $ROLES header values") {
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType())

      val requestRoles = Set("one", "two")
      val tokenRoles = Set("three", "four")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      requestRoles.foreach(request.appendHeader(ROLES, _))

      keystoneV2AuthorizationFilter.scopeRolesHeader(request, tokenRoles)

      request.getSplittableHeaderScala(ROLES) should contain only (tokenRoles.toSeq: _*)
    }
  }

  describe("scopeTenantToRolesMapHeader") {
    it(s"should set the $TENANT_ROLES_MAP header to the provided map") {
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType())

      val tenantToRoles = Map("tenant1" -> Set("role1", "role2"), "tenant2" -> Set("role3"))
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)

      keystoneV2AuthorizationFilter.scopeTenantToRolesMapHeader(request, tenantToRoles)

      Json.parse(Base64Helper.base64DecodeUtf8(request.getHeader(TENANT_ROLES_MAP))) shouldEqual Json.toJson(tenantToRoles)
    }

    it(s"should overwrite any existing value of the $TENANT_ROLES_MAP header") {
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType())

      val tenantToRoles = Map("tenant1" -> Set("role1", "role2"), "tenant2" -> Set("role3"))
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      request.addHeader(TENANT_ROLES_MAP, Base64Helper.base64EncodeUtf8("""{"myTenant":["myRole"]}"""))

      keystoneV2AuthorizationFilter.scopeTenantToRolesMapHeader(request, tenantToRoles)

      request.getHeadersScala(TENANT_ROLES_MAP) should have size 1
      Json.parse(Base64Helper.base64DecodeUtf8(request.getHeader(TENANT_ROLES_MAP))) shouldEqual Json.toJson(tenantToRoles)
    }

    it(s"should overwrite any existing value of the $TENANT_ROLES_MAP header even if the new map is empty") {
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType())

      val tenantToRoles = Map.empty[String, Set[String]]
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      request.addHeader(TENANT_ROLES_MAP, Base64Helper.base64EncodeUtf8("""{"myTenant":["myRole"]}"""))

      keystoneV2AuthorizationFilter.scopeTenantToRolesMapHeader(request, tenantToRoles)

      request.getHeadersScala(TENANT_ROLES_MAP) should have size 1
      Json.parse(Base64Helper.base64DecodeUtf8(request.getHeader(TENANT_ROLES_MAP))) shouldEqual Json.toJson(tenantToRoles)
    }

    it(s"should not modify the $TENANT_ROLES_MAP header when configured to send all tenant IDs") {
      val sendAllTenantIds = true
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withSendAllTenantIds(sendAllTenantIds))

      val tenantToRoles = Map("tenant1" -> Set("role1", "role2"), "tenant2" -> Set("role3"))
      val existingHeaderValue = Base64Helper.base64EncodeUtf8("""{"myTenant":["myRole"]}""")
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      request.addHeader(TENANT_ROLES_MAP, existingHeaderValue)

      keystoneV2AuthorizationFilter.scopeTenantToRolesMapHeader(request, tenantToRoles)

      request.getHeadersScala(TENANT_ROLES_MAP) should have size 1
      request.getHeader(TENANT_ROLES_MAP) shouldEqual existingHeaderValue
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
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(UnparsableTenantException("Unparseable tenant")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_UNAUTHORIZED
    }

    it("should reject as an internal server error when a failure is caused by a missing token") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(MissingTenantToRolesMapException("Missing token")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_INTERNAL_SERVER_ERROR
    }

    it("should reject as an internal server error when a failure is caused by a missing endpoints object") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(MissingEndpointsException("Missing endpoints")))

      result shouldBe a[Reject]
      result.asInstanceOf[Reject].status shouldEqual SC_INTERNAL_SERVER_ERROR
    }

    it("should reject as an internal server error when a failure is caused by an invalid token") {
      val result = keystoneV2AuthorizationFilter.handleFailures(Failure(InvalidTenantToRolesMapException("Invalid token")))

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
      val miscTenantIds = Set("hammer2", "bolt3")
      keystoneV2AuthorizationFilter.configuration = new KeystoneV2Config()
        .withTenantHandling(new TenantHandlingType()
          .withValidateTenant(new ValidateTenantType()
              .withHeaderExtractionName(inputTenantHeaderName)))

      val tenantToRolesMap = Map(userTenantId -> Set.empty[String])
      val tenantToRolesJson = Json.stringify(Json.toJson(tenantToRolesMap))
      val encodedTenantToRolesJson = Base64Helper.base64EncodeUtf8(tenantToRolesJson)
      val request = new HttpServletRequestWrapper(new MockHttpServletRequest)
      request.addHeader(TENANT_ROLES_MAP, encodedTenantToRolesJson)
      request.addHeader(inputTenantHeaderName, userTenantId)
      miscTenantIds.foreach(request.addHeader(TENANT_ID, _))

      val result = keystoneV2AuthorizationFilter.doAuth(request)

      result.success
      request.getHeadersScala(TENANT_ID) should contain only userTenantId
    }
  }
}

object KeystoneV2AuthorizationFilterTest {
  implicit val endpointWrites: Writes[Endpoint] = (
    (JsPath \ "region").writeNullable[String] and
      (JsPath \ "name").writeNullable[String] and
      (JsPath \ "type").writeNullable[String] and
      (JsPath \ "publicURL").write[String]
    ) (unlift(Endpoint.unapply))
}
