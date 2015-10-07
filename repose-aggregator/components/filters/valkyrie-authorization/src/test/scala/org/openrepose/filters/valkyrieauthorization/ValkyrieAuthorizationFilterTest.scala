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
package org.openrepose.filters.valkyrieauthorization

import java.io.ByteArrayInputStream
import java.net.URL
import java.util
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse
import javax.servlet.{FilterChain, ServletRequest, ServletResponse}

import com.mockrunner.mock.web.{MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import com.rackspace.httpdelegation.{HttpDelegationHeaderNames, HttpDelegationManager}
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.openrepose.commons.utils.http.{CommonHttpHeader, ServiceClientResponse}
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientException}
import org.openrepose.filters.valkyrieauthorization.config.DevicePath.Regex
import org.openrepose.filters.valkyrieauthorization.config._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ValkyrieAuthorizationFilterTest extends FunSpec with BeforeAndAfter with MockitoSugar with HttpDelegationManager {
  //todo: I suspect some of these tests are repetitive now, although they test it from different perspectives so
  // probably still worthwhile. I think some describe mocking behavior where it's not neccessary as well. Short
  // timelines mean i can't dig into them right now.
  val akkaServiceClient = mock[AkkaServiceClient]
  val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore: Datastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)

  before {
    Mockito.reset(mockDatastore)
    Mockito.reset(akkaServiceClient)
  }

  describe("when initializing the filter") {
    it("should initialize the configuration to a given configuration") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, akkaServiceClient, mockDatastoreService)

      val config: MockFilterConfig = new MockFilterConfig
      config.setFilterName("ValkyrieFilter")

      filter.init(config)

      val resourceCaptor = ArgumentCaptor.forClass(classOf[URL])
      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.eq("ValkyrieFilter"),
        Matchers.eq("valkyrie-authorization.cfg.xml"),
        resourceCaptor.capture,
        Matchers.eq(filter),
        Matchers.eq(classOf[ValkyrieAuthorizationConfig]))

      assert(resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/valkyrie-authorization.xsd"))
    }

    it("should initialize the configuration to a given name") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, akkaServiceClient, mockDatastoreService)

      val config: MockFilterConfig = new MockFilterConfig
      config.setInitParameter("filter-config", "another-name.cfg.xml")

      filter.init(config)

      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.anyString,
        Matchers.eq("another-name.cfg.xml"),
        Matchers.any(classOf[URL]),
        Matchers.any(classOf[ValkyrieAuthorizationFilter]),
        Matchers.eq(classOf[ValkyrieAuthorizationConfig]))
    }
  }

  describe("when destroying the filter") {
    it("should deregister the configuration from the configuration service") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, akkaServiceClient, mockDatastoreService)

      val config: MockFilterConfig = new MockFilterConfig
      filter.init(config)
      filter.destroy

      Mockito.verify(mockConfigService).unsubscribeFrom("valkyrie-authorization.cfg.xml", filter)
    }
  }

  describe("when the configuration is updated") {
    it("should set the current configuration on the filter with the defaults initially and flag that it is initialized") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)

      assert(!filter.isInitialized)

      val configuration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(configuration)

      assert(filter.configuration == configuration)
      assert(filter.configuration.getDelegating == null)
      assert(filter.configuration.getCacheTimeoutMillis == 300000)
      assert(filter.isInitialized)
    }

    it("should set the default delegation quality to .1") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)

      assert(filter.configuration == null)

      val configuration = new ValkyrieAuthorizationConfig
      val delegation = new DelegatingType
      configuration.setDelegating(delegation)
      filter.configurationUpdated(configuration)

      assert(filter.configuration.getDelegating.getQuality == .1)
    }

    it("should set the configuration to current and update the cache timeout") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)

      val configuration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(configuration)

      assert(filter.configuration == configuration)
      assert(filter.isInitialized)

      val newConfiguration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(newConfiguration)

      assert(filter.configuration == newConfiguration)
      assert(filter.isInitialized)
    }
  }

  describe("when a request to authorize occurs") {
    case class RequestProcessor(method: String, headers: Map[String, String], url: String = "http://foo.com:8080")
    case class ValkyrieResponse(code: Int, payload: String)
    case class Result(code: Int, message: String)

    List((RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product")))), //View role
      (RequestProcessor("HEAD", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product")))), //Without colon in tenant
      (RequestProcessor("POST", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "edit_product")))), //Edit role
      (RequestProcessor("PUT", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "admin_product")))), //Admin role
      (RequestProcessor("PUT", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "98765", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(accountPermissions("account_admin", "butts_permission"), devicePermissions("123456", "admin_product")))) //account Admin role
    ).foreach { case (request, valkyrie) =>
      it(s"should allow requests for $request with Valkyrie response of $valkyrie") {
        setMockAkkaBehavior("someTenant", request.headers.getOrElse("X-Contact-Id", "ThisIsMissingAContact"), valkyrie.code, valkyrie.payload)

        val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
        filter.configurationUpdated(createGenericValkyrieConfiguration(null))

        val mockServletRequest = new MockHttpServletRequest
        mockServletRequest.setMethod(request.method)
        mockServletRequest.setRequestURL(request.url)
        request.headers.foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

        val mockFilterChain = mock[FilterChain]
        filter.doFilter(mockServletRequest, new MockHttpServletResponse, mockFilterChain)

        val responseCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletResponse])
        Mockito.verify(mockFilterChain).doFilter(Matchers.any(classOf[ServletRequest]), responseCaptor.capture())
        assert(responseCaptor.getValue.getStatus == 200)
      }
    }

    List((RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/foo"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product")))), //View role
      (RequestProcessor("HEAD", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/foo"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product")))), //Without colon in tenant
      (RequestProcessor("POST", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/foo"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "edit_product")))), //Edit role
      (RequestProcessor("PUT", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/foo"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "admin_product")))), //Admin role
      (RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/bar"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product")))), //View role
      (RequestProcessor("HEAD", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/bar"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product")))), //Without colon in tenant
      (RequestProcessor("POST", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/bar"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "edit_product")))), //Edit role
      (RequestProcessor("PUT", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Contact-Id" -> "123456"), "http://foo.com:8080/bar"), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "admin_product")))) //Admin role
    ).foreach { case (request, valkyrie) =>
      it(s"should allow requests for $request with Valkyrie response of $valkyrie without device id when on either accepted list") {
        setMockAkkaBehavior("someTenant", request.headers.getOrElse("X-Contact-Id", "ThisIsMissingAContact"), valkyrie.code, valkyrie.payload)

        val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
        filter.configurationUpdated(createGenericValkyrieConfiguration(null))

        val mockServletRequest = new MockHttpServletRequest
        mockServletRequest.setMethod(request.method)
        mockServletRequest.setRequestURL(request.url)
        request.headers.foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

        val mockFilterChain = mock[FilterChain]
        Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
          override def answer(invocation: InvocationOnMock): Unit =
            invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("98765", "123456"))
        })

        val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
        filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

        assert(originalResponse.getStatus == 200)
      }
    }

    List((RequestProcessor("GET", Map("X-Tenant-Id" -> "application:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("111111", "view_product"))), Result(403, "Not Authorized")), //Not a hybrid tenant
      (RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("111111", "view_product"))), Result(403, "Not Authorized")), //Non matching device
      (RequestProcessor("PUT", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "view_product"))), Result(403, "Not Authorized")), //Non matching role
      (RequestProcessor("PUT", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("123456", "not_a_role"))), Result(403, "Not Authorized")), //Not a real role
      (RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(403, ""), Result(502, "Valkyrie returned a 403")), //Bad Permissions to Valkyrie
      (RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456")), ValkyrieResponse(404, ""), Result(401, "No contact ID specified")), //Missing Contact
      (RequestProcessor("GET", Map("X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(404, ""), Result(401, "No tenant ID specified")), //Missing Tenant
      (RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, createValkyrieResponse(devicePermissions("", "view_product"))), Result(502, "Invalid Json response from Valkyrie")), //Malformed Valkyrie Response - Missing Device
      (RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456")), ValkyrieResponse(200, "I'm not really json"), Result(502, "Invalid Json response from Valkyrie")) //Malformed Valkyrie Response - Bad Json
    ).foreach { case (request, valkyrie, result) =>
      List(null, new DelegatingType).foreach { delegation =>
        val delegating = Option(delegation).isDefined
        it(s"should be ${result.code} where delegation is $delegating for $request with Valkyrie response of $valkyrie") {
          setMockAkkaBehavior("someTenant", request.headers.getOrElse("X-Contact-Id", "ThisIsMissingAContact"), valkyrie.code, valkyrie.payload)

          val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
          filter.configurationUpdated(createGenericValkyrieConfiguration(delegation))

          val mockServletRequest = new MockHttpServletRequest
          mockServletRequest.setMethod(request.method)
          mockServletRequest.setRequestURL(request.url)
          request.headers.foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

          val mockServletResponse = new MockHttpServletResponse
          val mockFilterChain = mock[FilterChain]
          filter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain)

          if (Option(delegation).isDefined) {
            assert(mockServletResponse.getStatusCode == 200)
            val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
            Mockito.verify(mockFilterChain).doFilter(requestCaptor.capture(), Matchers.any(classOf[ServletResponse]))
            val delegationHeaders: Map[String, List[String]] = buildDelegationHeaders(result.code, "valkyrie-authorization", result.message, .1)
            assert(requestCaptor.getValue.getHeaders(HttpDelegationHeaderNames.Delegated).toList == delegationHeaders.get(HttpDelegationHeaderNames.Delegated).get)
          } else {
            assert(mockServletResponse.getStatusCode == result.code)
          }
        }
      }
    }

    List(null, new DelegatingType).foreach { delegation =>
      val delegating = Option(delegation).isDefined
      it(s"should return a 502 and delegation is $delegating with appropriate message when unable to communicate with Valkyrie") {
        Mockito.when(akkaServiceClient.get(Matchers.any(), Matchers.any(), Matchers.any())).thenThrow(new AkkaServiceClientException("Valkyrie is missing", new Exception()))

        val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
        filter.configurationUpdated(createGenericValkyrieConfiguration(delegation))

        val mockServletRequest = new MockHttpServletRequest
        mockServletRequest.setMethod("GET")
        mockServletRequest.setRequestURL("http://foo.com:8080")
        Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456").foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

        val mockFilterChain = mock[FilterChain]
        val mockServletResponse = new MockHttpServletResponse
        filter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain)

        if (Option(delegation).isDefined) {
          assert(mockServletResponse.getStatusCode == 200)
          val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
          Mockito.verify(mockFilterChain).doFilter(requestCaptor.capture(), Matchers.any(classOf[ServletResponse]))
          val delegationHeaders: Map[String, List[String]] = buildDelegationHeaders(502, "valkyrie-authorization", "Unable to communicate with Valkyrie: Valkyrie is missing", .1)
          assert(requestCaptor.getValue.getHeaders(HttpDelegationHeaderNames.Delegated).toList == delegationHeaders.get(HttpDelegationHeaderNames.Delegated).get)
        } else {
          assert(mockServletResponse.getStatusCode == 502)
        }
      }
    }

    it("should bypasses validation if the user has a role listed in pre-authorized-roles") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)

      val configuration = createGenericValkyrieConfiguration(null)
      val preAuthorizedRoles: RolesList = new RolesList
      val roles: util.List[String] = preAuthorizedRoles.getRole
      val superRootAdminUser = "superRootAdminUser"
      roles.add(superRootAdminUser)
      configuration.setPreAuthorizedRoles(preAuthorizedRoles)

      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com:8080")
      mockServletRequest.setHeader("X-Roles", s"$superRootAdminUser,buttsRole")
      val mockServletResponse = new MockHttpServletResponse
      val mockFilterChain = mock[FilterChain]

      filter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain)

      assert(mockServletResponse.getStatusCode == 200)
    }

    it("should be able to cache the valkyrie permissions so we dont have to make repeated calls") {
      val request = RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "1234561", "X-Contact-Id" -> "123456"))
      setMockAkkaBehavior("someTenant",
        request.headers.getOrElse("X-Contact-Id", "ThisIsMissingAContact"),
        200,
        createValkyrieResponse(devicePermissions("123456", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      Mockito.when(mockDatastore.get("VALKYRIE-FILTERanysomeTenant123456")).thenAnswer(new Answer[Serializable] {
        var firstAttempt = true

        override def answer(invocation: InvocationOnMock): Serializable =
          if (firstAttempt) {
            firstAttempt = false
            null
          } else {
            filter.UserPermissions(Vector.empty[String], Vector(filter.DeviceToPermission(123456, "view_product"), filter.DeviceToPermission(1234561, "view_product1"))).asInstanceOf[Serializable]
          }
      })
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod(request.method)
      mockServletRequest.setRequestURL("http://foo.com:8080")
      request.headers.foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

      val mockServletResponse = new MockHttpServletResponse
      val mockFilterChain = mock[FilterChain]
      filter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain)
      assert(mockServletResponse.getStatusCode == 403)

      Mockito.verify(mockDatastore).put("VALKYRIE-FILTERanysomeTenant123456", filter.UserPermissions(Vector.empty[String], Vector(filter.DeviceToPermission(123456, "view_product"), filter.DeviceToPermission(1234561, "view_product1"))), 300000, TimeUnit.MILLISECONDS)

      val secondRequest = new MockHttpServletRequest
      val secondServletResponse = new MockHttpServletResponse
      val secondRequestProcessor = RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456"))
      secondRequest.setMethod(secondRequestProcessor.method)
      secondRequest.setRequestURL("http://foo.com:8080")
      secondRequestProcessor.headers.foreach { case (k, v) => secondRequest.setHeader(k, v) }
      filter.doFilter(secondRequest, secondServletResponse, mockFilterChain)
      assert(secondServletResponse.getStatusCode == 200)

      Mockito.verify(akkaServiceClient, Mockito.times(1)).get(
        "VALKYRIE-FILTERanysomeTenant" + request.headers.get("X-Contact-Id").get,
        s"http://foo.com:8080/account/someTenant/permissions/contacts/any/by_contact/${request.headers.get("X-Contact-Id").get}/effective",
        Map("X-Auth-User" -> "someUser", "X-Auth-Token" -> "somePassword"))
    }

    List(null, new DelegatingType).foreach { delegation =>
      it(s"should be able to mask 403 to a 404 ${
        if (delegation != null) {
          "using delegation"
        } else {
          ""
        }
      } ") {
        val request = RequestProcessor("PUT", Map("X-Tenant-Id" -> "application:someTenant", "X-Device-Id" -> "123456", "X-Contact-Id" -> "123456"))

        setMockAkkaBehavior("someTenant", request.headers.getOrElse("X-Contact-Id", "123456"), 200, createValkyrieResponse(devicePermissions("123456", "view_product")))

        val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
        val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(delegation)
        configuration.setEnableMasking403S(true)
        filter.configurationUpdated(configuration)

        val mockServletRequest = new MockHttpServletRequest
        mockServletRequest.setMethod(request.method)
        mockServletRequest.setRequestURL("http://foo.com:8080")
        request.headers.foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

        val mockServletResponse = new MockHttpServletResponse
        val mockFilterChain = mock[FilterChain]
        filter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain)

        if (Option(delegation).isDefined) {
          assert(mockServletResponse.getStatusCode == 200)
          val requestCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])
          Mockito.verify(mockFilterChain).doFilter(requestCaptor.capture(), Matchers.any(classOf[ServletResponse]))
          val delegationHeaders: Map[String, List[String]] = buildDelegationHeaders(404, "valkyrie-authorization", "Not Found", .1)
          assert(requestCaptor.getValue.getHeaders(HttpDelegationHeaderNames.Delegated).toList == delegationHeaders.get(HttpDelegationHeaderNames.Delegated).get)
        } else {
          assert(mockServletResponse.getStatusCode == 404)
        }
      }
    }

    it("should send a request guid to valkyrie if present in incoming request") {
      val request = RequestProcessor("GET", Map("X-Tenant-Id" -> "hybrid:someTenant", "X-Device-Id" -> "123456",
        "X-Contact-Id" -> "123456", CommonHttpHeader.TRACE_GUID.toString -> "test-guid"))
      Mockito.when(akkaServiceClient.get(
        "VALKYRIE-FILTERanysomeTenant123456",
        "http://foo.com:8080/account/someTenant/permissions/contacts/any/by_contact/123456/effective",
        Map("X-Auth-User" -> "someUser", "X-Auth-Token" -> "somePassword", CommonHttpHeader.TRACE_GUID.toString -> "test-guid")))
        .thenReturn(new ServiceClientResponse(200, new ByteArrayInputStream(createValkyrieResponse(devicePermissions("123456", "view_product")).getBytes)))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod(request.method)
      mockServletRequest.setRequestURL("http://foo.com:8080")
      request.headers.foreach { case (k, v) => mockServletRequest.setHeader(k, v) }

      val mockFilterChain = mock[FilterChain]
      filter.doFilter(mockServletRequest, new MockHttpServletResponse, mockFilterChain)

      Mockito.verify(akkaServiceClient).get("VALKYRIE-FILTERanysomeTenant123456",
        "http://foo.com:8080/account/someTenant/permissions/contacts/any/by_contact/123456/effective",
        Map("X-Auth-User" -> "someUser", "X-Auth-Token" -> "somePassword", CommonHttpHeader.TRACE_GUID.toString -> "test-guid"))
    }
  }

  describe("when permission to role translation is turned on and request includes a Device ID") {
    val config = createGenericValkyrieConfiguration(null)
    config.setTranslatePermissionsToRoles(new Object)
    val tenantId = "hybrid:98765"
    val transformedTenant = "98765"
    val contactId = "123456"
    val deviceId = "98765"
    val filterChain = mock[FilterChain]
    val mockServletRequest = new MockHttpServletRequest
    val mockServletResponse = new MockHttpServletResponse
    val filter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
    filter.configurationUpdated(config)

    def setup() = {
      mockServletRequest.resetAll()
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com:8080")
      mockServletRequest.setHeader("X-Tenant-Id", tenantId)
      mockServletRequest.setHeader("X-Contact-Id", contactId)

      mockServletResponse.resetAll()

      Mockito.reset(filterChain)
    }

    List("view_product", "edit_product", "admin_product").foreach { devicePermission =>
      it(s"should translate permissions to roles with device permission $devicePermission") {
        setup()
        val devices = devicePermissions(deviceId, devicePermission)
        mockServletRequest.setHeader("X-Device-Id", deviceId)
        setMockAkkaBehavior(transformedTenant, contactId, 200,
          createValkyrieResponse(accountPermissions("some_permission", "a_different_permission"), devices))
        val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

        filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

        Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))
        val roles: Iterator[String] = captor.getValue.getHeaders("X-Roles").asScala
        assert(roles.contains("some_permission"))
        assert(roles.contains("a_different_permission"))
        assert(roles.contains(devicePermission))
      }
    }

    List("remember_product", "consider_product", "admire_product").foreach { devicePermission =>
      it(s"should not be authorized to translate permissions to roles with device permission $devicePermission") {
        setup()
        val devices = devicePermissions(deviceId, devicePermission)
        mockServletRequest.setHeader("X-Device-Id", deviceId)
        setMockAkkaBehavior(transformedTenant, contactId, 200,
          createValkyrieResponse(accountPermissions("some_permission", "a_different_permission"), devices))

        filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

        assert(mockServletResponse.getStatusCode == 403)
      }
    }
  }

  describe("when permission to role translation is turned on") {
    val config = createGenericValkyrieConfiguration(null)
    config.setTranslatePermissionsToRoles(new Object)
    val tenantId = "hybrid:98765"
    val transformedTenant = "98765"
    val contactId = "123456"
    val filterChain = mock[FilterChain]
    val mockServletRequest = new MockHttpServletRequest
    val mockServletResponse = new MockHttpServletResponse
    val devices = devicePermissions("98765", "view_product")
    val filter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
    filter.configurationUpdated(config)

    def setup() = {
      mockServletRequest.resetAll()
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com:8080")
      mockServletRequest.setHeader("X-Tenant-Id", tenantId)
      mockServletRequest.setHeader("X-Contact-Id", contactId)

      mockServletResponse.resetAll()

      Mockito.reset(filterChain)
    }

    it("should translate permissions to roles") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission"), devices))
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))
      val roles: Iterator[String] = captor.getValue.getHeaders("X-Roles").asScala
      assert(roles.contains("some_permission"))
      assert(roles.contains("a_different_permission"))
    }

    it("should 401 when tenant id isn't present") {
      setup()
      mockServletRequest.clearHeaders()
      mockServletRequest.setHeader("X-Contact-Id", contactId)
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission"), devices))

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 401)
    }

    it("should 401 when contact id isn't present") {
      setup()
      mockServletRequest.clearHeaders()
      mockServletRequest.setHeader("X-Tenant-Id", tenantId)
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission"), devices))

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 401)
    }

    it("should 403 when tenant is non-hybrid") {
      setup()
      mockServletRequest.clearHeaders()
      mockServletRequest.setHeader("X-Tenant-Id", "987654")
      mockServletRequest.setHeader("X-Contact-Id", contactId)
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission"), devices))

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 403)
    }

    it("should 502 when valkyrie 404s") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 404, "Not found")

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 502)
    }

    it("should 502 when valkyrie 500s") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 500, "Internal Server Error")

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 502)
    }

    it("should 502 when valkyrie gives an unexpected response") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 200, """{"banana":"phone"}""")

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 502)
    }

    it("should 502 when we have an exception while talking to valkyrie") {
      setup()
      Mockito.when(akkaServiceClient.get(Matchers.any(classOf[String]),
        Matchers.eq(s"http://foo.com:8080/account/$transformedTenant/permissions/contacts/any/by_contact/$contactId/effective"),
        Matchers.any(classOf[java.util.Map[String, String]]))).thenThrow(new AkkaServiceClientException("test exception", null))

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      assert(mockServletResponse.getStatus == 502)
    }

    it("should use the values from the datastore when available") {
      setup()
      Mockito.when(mockDatastore.get(ValkyrieAuthorizationFilter.CACHE_PREFIX + "any" + transformedTenant + contactId))
        .thenReturn(filter.UserPermissions(Vector("some_permission", "a_different_permission"), Vector.empty[filter.DeviceToPermission]), Nil:_*)
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      val roles: Iterator[String] = captor.getValue.getHeaders("X-Roles").asScala
      assert(roles.contains("some_permission"))
      assert(roles.contains("a_different_permission"))

    }
  }

  describe("when permission to role translation and delegation is turned on") {
    val config = createGenericValkyrieConfiguration(null)
    config.setTranslatePermissionsToRoles(new Object)
    config.setDelegating(new DelegatingType)
    config.setCollectionResources(null)
    val tenantId = "hybrid:98765"
    val transformedTenant = "98765"
    val contactId = "123456"
    val filterChain = mock[FilterChain]
    val mockServletRequest = new MockHttpServletRequest
    val mockServletResponse = new MockHttpServletResponse
    val filter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
    filter.configurationUpdated(config)

    def setup() = {
      mockServletRequest.resetAll()
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com:8080")
      mockServletRequest.setHeader("X-Tenant-Id", tenantId)
      mockServletRequest.setHeader("X-Contact-Id", contactId)

      mockServletResponse.resetAll()

      Mockito.reset(filterChain)
    }

    it("should translate permissions to roles") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission")))
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))
      val roles: Iterator[String] = captor.getValue.getHeaders("X-Roles").asScala
      assert(roles.contains("some_permission"))
      assert(roles.contains("a_different_permission"))
    }

    it("should 401 when tenant id isn't present") {
      setup()
      mockServletRequest.clearHeaders()
      mockServletRequest.setHeader("X-Contact-Id", contactId)
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission")))
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("401"))
    }

    it("should 401 when contact id isn't present") {
      setup()
      mockServletRequest.clearHeaders()
      mockServletRequest.setHeader("X-Tenant-Id", tenantId)
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission")))
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("401"))
    }

    it("should 403 when tenant is non-hybrid") {
      setup()
      mockServletRequest.clearHeaders()
      mockServletRequest.setHeader("X-Tenant-Id", "987654")
      mockServletRequest.setHeader("X-Contact-Id", contactId)
      setMockAkkaBehavior(transformedTenant, contactId, 200, createValkyrieResponse(accountPermissions("some_permission", "a_different_permission")))
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("403"))
    }

    it("should 502 when valkyrie 404s") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 404, "Not found")
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("502"))
    }

    it("should 502 when valkyrie 500s") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 500, "Internal Server Error")
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("502"))
    }

    it("should 502 when valkyrie gives an unexpected response") {
      setup()
      setMockAkkaBehavior(transformedTenant, contactId, 200, """{"banana":"phone"}""")
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("502"))
    }

    it("should 502 when we have an exception while talking to valkyrie") {
      setup()
      Mockito.when(akkaServiceClient.get(Matchers.any(classOf[String]),
        Matchers.eq(s"http://foo.com:8080/account/$transformedTenant/permissions/contacts/any/by_contact/$contactId/effective"),
        Matchers.any(classOf[java.util.Map[String, String]]))).thenThrow(new AkkaServiceClientException("test exception", null))
      val captor = ArgumentCaptor.forClass(classOf[MutableHttpServletRequest])

      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)
      Mockito.verify(filterChain).doFilter(captor.capture(), Matchers.any(classOf[ServletResponse]))

      assert(mockServletResponse.getStatus == 200)
      assert(captor.getValue.getHeader(HttpDelegationHeaderNames.Delegated).contains("502"))
    }
  }

  describe("do filter should cull appropriately") {
    import play.api.libs.json._

    it("should remove some of the values") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("98765", "123456"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      val content: String = originalResponse.getOutputStreamContent
      val json: JsValue = Json.parse(content)
      assert((json \ "values").asInstanceOf[JsArray].value.size == 1)
      assert((json \ "metadata" \ "count").asInstanceOf[JsNumber].as[Int] == 1)
    }

    it("should remove all values") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("234567", "123456"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      val content: String = originalResponse.getOutputStreamContent
      val json: JsValue = Json.parse(content)
      assert((json \ "values").asInstanceOf[JsArray].value.size == 0)
      assert((json \ "metadata" \ "count").asInstanceOf[JsNumber].as[Int] == 0)
    }

    it("should remove no values") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("98765", "98765"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      val content: String = originalResponse.getOutputStreamContent
      val json: JsValue = Json.parse(content)
      assert((json \ "values").asInstanceOf[JsArray].value.size == 2)
      assert((json \ "metadata" \ "count").asInstanceOf[JsNumber].as[Int] == 2)
    }

    it("should remove no values for account admins") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(accountPermissions("account_admin", "butts_permission")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("98765", "98765"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      val content: String = originalResponse.getOutputStreamContent
      val json: JsValue = Json.parse(content)
      assert((json \ "values").asInstanceOf[JsArray].value.size == 2)
      assert((json \ "metadata" \ "count").asInstanceOf[JsNumber].as[Int] == 2)
    }

    it("should remove no values for non-matching resources") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      filter.configurationUpdated(createGenericValkyrieConfiguration(null))

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/foo")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("123456", "345678"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      val content: String = originalResponse.getOutputStreamContent
      val json: JsValue = Json.parse(content)
      assert((json \ "values").asInstanceOf[JsArray].value.size == 2)
      assert((json \ "metadata" \ "count").asInstanceOf[JsNumber].as[Int] == 2)
    }

    it("should throw a 500 when the regex is un-parseable") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(null)
      configuration.getCollectionResources.getResource.get(0).getCollection.get(0).getJson.getPathToDeviceId.getRegex.setValue("*/*")
      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("123456", "345678"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      assert(originalResponse.getStatusCode == 500)

    }

    it("should throw a 500 when the capture group is to large") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(null)
      configuration.getCollectionResources.getResource.get(0).getCollection.get(0).getJson.getPathToDeviceId.getRegex.setCaptureGroup(52)
      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("123456", "345678"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      assert(originalResponse.getStatusCode == 500)
    }

    it("should throw a 500 when the path for the collection is bad") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(null)
      configuration.getCollectionResources.getResource.get(0).getCollection.get(0).getJson.setPathToCollection("$.butts")
      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("123456", "345678"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      assert(originalResponse.getStatusCode == 500)
    }

    it("should throw a 500 when the path for the device id is bad") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(null)
      configuration.getCollectionResources.getResource.get(0).getCollection.get(0).getJson.getPathToDeviceId.setPath("$.butts")
      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("123456", "345678"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      assert(originalResponse.getStatusCode == 500)
    }

    it("should throw a 500 when the path for the count is bad") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(null)
      configuration.getCollectionResources.getResource.get(0).getCollection.get(0).getJson.setPathToItemCount("$.butts")
      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print(createOriginServiceResponse("123456", "345678"))
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      assert(originalResponse.getStatusCode == 500)
    }

    it("should throw a 500 when the response contains bad json") {
      setMockAkkaBehavior("someTenant", "123456", 200, createValkyrieResponse(devicePermissions("98765", "view_product")))

      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient, mockDatastoreService)
      val configuration: ValkyrieAuthorizationConfig = createGenericValkyrieConfiguration(null)
      filter.configurationUpdated(configuration)

      val mockServletRequest = new MockHttpServletRequest
      mockServletRequest.setMethod("GET")
      mockServletRequest.setRequestURL("http://foo.com/bar")
      mockServletRequest.setHeader("X-Contact-Id", "123456")
      mockServletRequest.setHeader("X-Tenant-Id", "hybrid:someTenant")

      val mockFilterChain = mock[FilterChain]
      val originalResponse: MockHttpServletResponse = new MockHttpServletResponse
      Mockito.when(mockFilterChain.doFilter(Matchers.any(classOf[ServletRequest]), Matchers.any(classOf[ServletResponse])))
        .thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit =
          invocation.getArguments()(1).asInstanceOf[HttpServletResponse].getOutputStream.print("butts")
      })

      filter.doFilter(mockServletRequest, originalResponse, mockFilterChain)

      assert(originalResponse.getStatusCode == 500)
    }
  }

  def createGenericValkyrieConfiguration(delegation: DelegatingType): ValkyrieAuthorizationConfig = {
    val configuration = new ValkyrieAuthorizationConfig
    val server = new ValkyrieServer
    server.setUri("http://foo.com:8080")
    server.setUsername("someUser")
    server.setPassword("somePassword")
    configuration.setValkyrieServer(server)
    configuration.setDelegating(delegation)
    val resource: Resource = new Resource
    resource.setPathRegex("/bar")
    val pathTriplet: PathTriplet = new PathTriplet
    pathTriplet.setPathToCollection("$.values")
    val devicePath: DevicePath = new DevicePath()
    devicePath.setPath("$.uri")
    val regex: Regex = new Regex
    regex.setValue("http://core.rackspace.com/accounts/\\d*/devices/(\\d*)")
    regex.setCaptureGroup(1)
    devicePath.setRegex(regex)
    pathTriplet.setPathToDeviceId(devicePath)
    pathTriplet.setPathToItemCount("$.metadata.count")
    val collection: Collection = new Collection
    collection.setJson(pathTriplet)
    resource.getCollection.add(collection)
    val collectionResources: CollectionResources = new CollectionResources
    collectionResources.getResource.add(resource)
    configuration.setCollectionResources(collectionResources)
    configuration
  }

  def setMockAkkaBehavior(tenant: String, contactHeader: String, valkyrieCode: Int, valkyriePayload: String): Unit = {
    Mockito.when(akkaServiceClient.get(
      ValkyrieAuthorizationFilter.CACHE_PREFIX + "any" + tenant + contactHeader,
      s"http://foo.com:8080/account/$tenant/permissions/contacts/any/by_contact/$contactHeader/effective",
      Map("X-Auth-User" -> "someUser", "X-Auth-Token" -> "somePassword")))
      .thenReturn(new ServiceClientResponse(valkyrieCode, new ByteArrayInputStream(valkyriePayload.getBytes)))
  }

  def createValkyrieResponse(permissions: String*): String = {
    s"""{
        "contact_permissions" :[
         ${permissions.mkString(",")}
         ]
       }"""
  }

  def devicePermissions(deviceId: String, permissionName: String): String = {
    s"""{
         "account_number":862323,
         "contact_id": 818029,
         "id": 0,
         ${if (deviceId != "") "\"item_id\": " + deviceId + "," else ""}
         "item_type_id" : 1,
         "item_type_name" : "devices",
         "permission_name" : "$permissionName",
         "permission_type_id" : 12
       }, {
         "account_number":862323,
         "contact_id": 818029,
         "id": 0,
         "item_id": ${deviceId}1,
         "item_type_id" : 1,
         "item_type_name" : "devices",
         "permission_name" : "${permissionName}1",
         "permission_type_id" : 12
       }"""
  }

  def accountPermissions(permission1: String, permission2: String): String = {
    s"""{
             "item_type_id": 2,
             "permission_type_id": 5,
             "item_type_name": "accounts",
             "contact_id": 817203,
             "account_number": 862323,
             "permission_name": "$permission1",
             "item_id": 862323,
             "id": 0
         },
         {
             "item_type_id": 2,
             "permission_type_id": 9,
             "item_type_name": "accounts",
             "contact_id": 817203,
             "account_number": 862323,
             "permission_name": "$permission2",
             "item_id": 862323,
             "id": 0
         }"""
  }

  def createOriginServiceResponse(deviceId1: String, deviceId2: String): String = {
    s"""{
        "values": [
            {
                "id": "en6bShuX7a",
                "label": "brad@morgabra.com",
                "ip_addresses": null,
                "metadata": {
                    "userId": "325742",
                    "email": "brad@morgabra.com"
                },
                "managed": false,
                "uri": "http://core.rackspace.com/accounts/1234/devices/$deviceId1",
                "agent_id": "e333a7d9-6f98-43ea-aed3-52bd06ab929f",
                "active_suppressions": [],
                "scheduled_suppressions": [],
                "created_at": 1405963090100,
                "updated_at": 1409247144717
            },
            {
                "id": "enADqSly1y",
                "label": "test",
                "ip_addresses": null,
                "metadata": null,
                "managed": false,
                "uri": "http://core.rackspace.com/accounts/1234/devices/$deviceId2",
                "agent_id": null,
                "active_suppressions": [],
                "scheduled_suppressions": [],
                "created_at": 1411055897191,
                "updated_at": 1411055897191
            }
        ],
        "metadata": {
            "count": 2,
            "limit": 2,
            "marker": null,
            "next_marker": "enB11JvqNv",
            "next_href": "https://monitoring.api.rackspacecloud.com/v1.0/731078/entities?limit=2&marker=enB11JvqNv"
        }
    }"""
  }
}
