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
package org.openrepose.filters.openstackidentityv3

import java.util.{Calendar, GregorianCalendar}
import javax.servlet.http.HttpServletResponse._

import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mockitoEq}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.{HttpDate, OpenStackServiceHeader}
import org.openrepose.commons.utils.servlet.filter.FilterAction
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config.SendProjectIdQuality
import org.openrepose.filters.openstackidentityv3.config._
import org.openrepose.filters.openstackidentityv3.objects._
import org.openrepose.filters.openstackidentityv3.utilities._
import org.scalatest._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.util.{Failure, Try}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3HandlerDeprecatedTest extends FunSpec with BeforeAndAfterEach with Matchers with PrivateMethodTester with MockitoSugar {

  private final val SC_TOO_MANY_REQUESTS = 429

  var identityV3Handler: OpenStackIdentityV3Handler = _
  var identityConfig: OpenstackIdentityV3Config = _
  var identityAPI: OpenStackIdentityV3API = _

  def defaultConfig(): Unit = {
    identityConfig = new OpenstackIdentityV3Config()
    identityConfig.setOpenstackIdentityService(new OpenstackIdentityService())
    identityConfig.getOpenstackIdentityService.setUsername("user")
    identityConfig.getOpenstackIdentityService.setPassword("password")
    identityConfig.getOpenstackIdentityService.setUri("http://test-uri.com")
    identityConfig.setServiceEndpoint(new ServiceEndpoint())
    identityConfig.getServiceEndpoint.setUrl("http://www.notreallyawebsite.com")
    identityConfig.setValidateProjectIdInUri(new ValidateProjectID())
    identityConfig.getValidateProjectIdInUri.setRegex("""/foo/(\d+)""")
    identityConfig.getValidateProjectIdInUri.setStripTokenProjectPrefixes("foo:/bar:")
    identityConfig.setRolesWhichBypassProjectIdCheck(new PreAuthRolesList())
    identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("admin")
    identityConfig.setForwardGroups(false)
  }

  def defaultHeaderConfig(): Unit = {
    defaultConfig()
    identityConfig.setSendProjectIdQuality(new SendProjectIdQuality)
    identityConfig.getSendProjectIdQuality.setDefaultProjectQuality(0.9)
    identityConfig.getSendProjectIdQuality.setUriProjectQuality(0.7)
    identityConfig.getSendProjectIdQuality.setRolesProjectQuality(0.5)
    identityAPI = mock[OpenStackIdentityV3API]
    identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
  }

  override def beforeEach(): Unit = {
    defaultConfig()
    identityAPI = mock[OpenStackIdentityV3API]
    identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
  }

  describe("handleRequest") {
    it("should pass filter if uri is in the whitelist") {
      val whiteList = new WhiteList()
      whiteList.getUriPattern.add("/test1")
      whiteList.getUriPattern.add("/test2")
      identityConfig.setWhiteList(whiteList)

      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")

      identityV3Handler.handleRequest(new HttpServletRequestWrapper(mockRequest), new MockHttpServletResponse()) shouldBe FilterAction.PASS
    }

    it("should attempt validation if uri isn't in the whitelist") {
      val whiteList = new WhiteList()
      whiteList.getUriPattern.add("/test1")
      whiteList.getUriPattern.add("/test2")
      identityConfig.setWhiteList(whiteList)

      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test3")

      val mockResponse = new MockHttpServletResponse()

      identityV3Handler.handleRequest(new HttpServletRequestWrapper(mockRequest), mockResponse) shouldBe FilterAction.RETURN
      mockResponse.getStatus shouldBe SC_UNAUTHORIZED
    }

    it("should add the X-Default-Region if rax_default_region is available for the user") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = Some("ORD"),
          expiresAt = "1",
          projectId = None,
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin")))))
      val mockRequest = new HttpServletRequestWrapper(new MockHttpServletRequest())
      mockRequest.replaceHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, new MockHttpServletResponse())
      mockRequest.getHeadersScala("X-Default-Region") should contain only "ORD"
    }

    it("should not include X-Default-Region if rax_default_region is not available for the user") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = None,
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin")))))
      val mockRequest = new HttpServletRequestWrapper(new MockHttpServletRequest())
      mockRequest.replaceHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, new MockHttpServletResponse())
      mockRequest.getHeaderNamesScala should not contain "X-Default-Region"
    }

    it("should add the X-Impersonator-Name and X-Impersonator-ID headers if the impersonator's information is available") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = None,
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin")),
          impersonatorId = Some("ImpersonationId"),
          impersonatorName = Some("ImpersonationName"))))
      val mockRequest = new HttpServletRequestWrapper(new MockHttpServletRequest())
      mockRequest.replaceHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, new MockHttpServletResponse())
      mockRequest.getHeadersScala("X-Impersonator-Name") should contain only "ImpersonationName"
      mockRequest.getHeadersScala("X-Impersonator-Id") should contain only "ImpersonationId"
    }

    it("should not add the X-Impersonator-Name and X-Impersonator-ID headers if the impersonator's information is not available") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = None,
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin")))))
      val mockRequest = new HttpServletRequestWrapper(new MockHttpServletRequest())
      mockRequest.replaceHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, new MockHttpServletResponse())
      mockRequest.getHeaderNamesScala should (not contain "X-Impersonator-Name" and not contain "X-Impersonator-Id")
    }

    it("should non-destructively add the x-roles header") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = Some("ORD"),
          expiresAt = "1",
          projectId = None,
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin")))))
      val mockRequest = new HttpServletRequestWrapper(new MockHttpServletRequest())
      mockRequest.replaceHeader("X-Subject-Token", "123456")
      mockRequest.addHeader(OpenStackServiceHeader.ROLES, "foo")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)

      identityV3Handler.handleRequest(mockRequest, new MockHttpServletResponse())
      mockRequest.getHeadersScala(OpenStackServiceHeader.ROLES) should contain only("foo", "admin")
    }

    it("should set the x-project-id header to the uri project id value if it is set and send all project ids is not set/false") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = Some("ProjectIdToNotSee"),
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin", Some("ProjectToNotSee"))))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/12345")
      val wrappedRequest = new HttpServletRequestWrapper(mockRequest)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(wrappedRequest, new MockHttpServletResponse())
      wrappedRequest.getHeadersScala("X-Project-Id") should contain only "12345"
    }

    it("should set the x-project-id header to the default project id if send all project ids is not set/false and there is no uri project id validation") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = Some("DefaultProjectIdToSee"),
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin", Some("ProjectToNotSee"))))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/bar")
      val wrappedRequest = new HttpServletRequestWrapper(mockRequest)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(wrappedRequest, new MockHttpServletResponse())
      wrappedRequest.getHeadersScala("X-Project-Id") should contain only "DefaultProjectIdToSee"
    }

    it("should not set the x-project-id header if there is no default and send all project ids is not set/false and there is no uri project id validation") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = None,
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin", Some("ProjectToNotSee"))))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/bar")
      val wrappedRequest = new HttpServletRequestWrapper(mockRequest)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(wrappedRequest, new MockHttpServletResponse())
      wrappedRequest.getHeaderNamesScala should not contain "X-Project-Id"
    }

    it("should return default project id and roles project ids returned by identity as multiple x-project-id headers if all project ids is true and there is no uri project id validation") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = Some("ProjectIdFromProject"),
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin", Some("ProjectIdFromRoles"))))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/12345")
      val wrappedRequest = new HttpServletRequestWrapper(mockRequest)
      identityConfig.setSendAllProjectIds(true)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(wrappedRequest, new MockHttpServletResponse())
      wrappedRequest.getHeadersScala("X-Project-Id") should contain only("ProjectIdFromProject", "ProjectIdFromRoles")
    }

    it("should return all project ids returned by identity as multiple x-project-id headers if all project ids is true") {
      when(identityAPI.validateToken("123456", None)).thenReturn(
        Try(ValidToken(
          userId = None,
          userName = None,
          defaultRegion = None,
          expiresAt = "1",
          projectId = Some("ProjectIdFromProject"),
          projectName = None,
          catalogJson = None,
          catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
          roles = List(Role("admin", Some("ProjectIdFromRoles"), Some("RaxExtensionProjectId"))))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/12345")
      val wrappedRequest = new HttpServletRequestWrapper(mockRequest)
      identityConfig.setSendAllProjectIds(true)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(wrappedRequest, new MockHttpServletResponse())
      wrappedRequest.getHeadersScala("X-Project-Id") should contain only("ProjectIdFromProject", "ProjectIdFromRoles", "RaxExtensionProjectId")
    }

    val statusCodes = List(SC_REQUEST_ENTITY_TOO_LARGE, SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      it(s"should return a ${SC_SERVICE_UNAVAILABLE} when receiving $statusCode from the OpenStack Identity service") {
        val retryCalendar = new GregorianCalendar()
        retryCalendar.add(Calendar.SECOND, 5)
        val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
        when(identityAPI.validateToken("123456", None)).thenReturn(
          Failure(new IdentityServiceOverLimitException("Rate limited by OpenStack Identity service", statusCode, retryString)))
        val mockRequest = new MockHttpServletRequest()
        mockRequest.addHeader("X-Subject-Token", "123456")
        val wrappedRequest = new HttpServletRequestWrapper(mockRequest)
        val mockResponse = new MockHttpServletResponse()
        identityConfig.setForwardGroups(false)
        identityConfig.setValidateProjectIdInUri(null)
        identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
        identityV3Handler.handleRequest(wrappedRequest, mockResponse)
        mockResponse.getStatus shouldBe SC_SERVICE_UNAVAILABLE
        mockResponse.getHeaders(HttpHeaders.RETRY_AFTER) should contain only retryString
      }
    }
  }

  describe("handleResponse") {
    // TODO: Get this to work, or make it a system level test
    ignore("should set the appropriate response status") {
      val mockServletResponse = new MockHttpServletResponse()

      val responseStatus = "response-status-key"
      val responseWwwAuthenticate = "response-www-authenticate"
      val resultStatus = "result-status"
      val resultWwwAuthenticate = "result-www-authenticate"

      List(
        Map(
          responseStatus -> SC_OK,
          resultStatus -> SC_OK
        ),
        Map(
          responseStatus -> SC_FORBIDDEN,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> SC_FORBIDDEN,
          resultWwwAuthenticate -> "Keystone uri=http://test-uri.com"
        ),
        Map(
          responseStatus -> SC_UNAUTHORIZED,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> SC_FORBIDDEN,
          resultWwwAuthenticate -> "Keystone uri=http://test-uri.com"
        ),
        Map(
          responseStatus -> SC_UNAUTHORIZED,
          resultStatus -> SC_INTERNAL_SERVER_ERROR
        ),
        Map(
          responseStatus -> SC_NOT_IMPLEMENTED,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> SC_INTERNAL_SERVER_ERROR
        ),
        Map(
          responseStatus -> SC_NOT_IMPLEMENTED,
          resultStatus -> SC_NOT_IMPLEMENTED
        )
      ) foreach { parameterMap =>
        mockServletResponse.setStatus(parameterMap(responseStatus).asInstanceOf[Integer])
        if (parameterMap.get(responseWwwAuthenticate).isDefined) {
          mockServletResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, parameterMap(responseWwwAuthenticate).asInstanceOf[String])
        }

        val filterAction = identityV3Handler.handleResponse(mockServletResponse)

        filterAction should not be FilterAction.NOT_SET
        mockServletResponse.getStatus shouldBe parameterMap(resultStatus)
        if (parameterMap.get(resultWwwAuthenticate).isDefined) {
          mockServletResponse.getHeaders(HttpHeaders.WWW_AUTHENTICATE) should contain(parameterMap(resultWwwAuthenticate))
        }
      }
    }
  }

  describe("authenticate") {
    val authenticate = PrivateMethod[Try[ValidToken]]('authenticate)

    it("should return a Failure when the x-subject-token header is not present") {
      identityV3Handler invokePrivate authenticate(None, None) shouldBe a[Failure[_]]
      an[InvalidSubjectTokenException] should be thrownBy identityV3Handler.invokePrivate(authenticate(None, None)).get
    }
  }

  describe("writeProjectHeader") {
    val writeProjectHeader = PrivateMethod[Unit]('writeProjectHeader)
    val roles = List(Role(null, Option("12345"), null), Role(null, Option("67890"), null))

    it("should only provide the url project when the flag says to not write all") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), false, false, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(uriPid))
    }

    it("should provide all the projects when the flag says to write all") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), true, false, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid))
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq("12345"))
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq("67890"))
    }

    it("should handle when roles are present but project ids are missing") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      val roleList = List(Role(null, None, None), Role(null, None, None))
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roleList, Some(uriPid), true, false, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid))
    }

    it("should handle when project ids are missing and not missing") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      val roleList = List(Role(null, None, None), Role(null, Option("foo"), None), Role(null, None, None))
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roleList, Some(uriPid), true, false, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid))
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq("foo"))
    }

    it("should add qualities when flagged and not writing all") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      defaultHeaderConfig()
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), false, true, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(uriPid), mockitoEq(0.7))
    }

    it("should add default quality when uriPid and defaultPid are matching") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      defaultHeaderConfig()
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(defaultPid), false, true, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid), mockitoEq(0.9))
    }

    it("should add qualities when flagged and writing all") {
      val wrappedRequest = mock[HttpServletRequestWrapper]
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      defaultHeaderConfig()
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), true, true, wrappedRequest)
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid), mockitoEq(0.9))
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq("12345"), mockitoEq(0.5))
      verify(wrappedRequest).addHeader(mockitoEq("X-Project-ID"), mockitoEq("67890"), mockitoEq(0.5))
    }
  }

  describe("containsRequiredEndpoint") {
    val containsRequiredEndpoint = PrivateMethod[Boolean]('containsRequiredEndpoint)

    it("should return true when there is an endpoint that matches the url") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
        Endpoint(None, None, None, "http://www.notreallyawebsite.com")
      ) shouldBe true
    }

    it("should return true when there is an endpoint that matches the url with the project id appended") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.notreallyawebsite.com/tenantId")),
        Endpoint(None, None, None, "http://www.notreallyawebsite.com")
      ) shouldBe true
    }

    it("should return false when there isn't an endpoint that matches the url") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(None, None, None, "http://www.banana.com")),
        Endpoint(None, None, None, "http://www.notreallyawebsite.com")
      ) shouldBe false
    }

    it("Should return true when the url matches and region does") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(None, null, Option("DFW"), "http://www.notreallyawebsite.com")),
        Endpoint(None, None, Option("DFW"), "http://www.notreallyawebsite.com")
      ) shouldBe true
    }

    it("Should return false when the url matches and region doesn't") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(None, None, None, "http://www.notreallyawebsite.com")),
        Endpoint(None, None, Option("DFW"), "http://www.notreallyawebsite.com")
      ) shouldBe false
    }

    it("Should return true when the url matches and name does") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(Option("foo"), None, Option("DFW"), "http://www.notreallyawebsite.com")),
        Endpoint(Option("foo"), None, None, "http://www.notreallyawebsite.com")
      ) shouldBe true
    }

    it("Should return false when the url matches and name doesn't") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(Option("bar"), null, None, "http://www.notreallyawebsite.com")),
        Endpoint(Option("foo"), None, None, "http://www.notreallyawebsite.com")
      ) shouldBe false
    }

    it("Should return true when the url matches and interface does") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(Option("foo"), Option("foo"), Option("DFW"), "http://www.notreallyawebsite.com")),
        Endpoint(None, Option("foo"), None, "http://www.notreallyawebsite.com")
      ) shouldBe true
    }

    it("Should return false when the url matches and interface doesn't") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(None, None, None, "http://www.woot.com"), Endpoint(Option("bar"), None, None, "http://www.notreallyawebsite.com")),
        Endpoint(None, Option("foo"), None, "http://www.notreallyawebsite.com")
      ) shouldBe false
    }
  }

  describe("isAuthorized") {
    val isAuthorized = PrivateMethod[Boolean]('isAuthorized)

    it("should return true when not configured to check endpoints") {
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(new OpenstackIdentityService())
      config.getOpenstackIdentityService.setUri("")

      val handler = new OpenStackIdentityV3Handler(config, identityAPI)

      handler invokePrivate isAuthorized(ValidToken(None, None, None, null, null, null, null, null, null)) shouldBe true
    }

    it("should return true when configured and the endpoint is present") {
      val catalogEndpoints = List(Endpoint(None, None, None, "http://www.notreallyawebsite.com"))
      val authToken = ValidToken(None, None, None, null, None, None, None, catalogEndpoints, null)

      identityV3Handler invokePrivate isAuthorized(authToken) shouldBe true
    }

    it("should return false when configured and the endpoint is not present") {
      val catalogEndpoints = List(Endpoint(None, None, None, "http://www.woot.com"))
      val authToken = ValidToken(None, None, None, null, None, None, None, catalogEndpoints, null)

      identityV3Handler invokePrivate isAuthorized(authToken) shouldBe false
    }
  }

  describe("base64Encode") {
    val base64Encode = PrivateMethod[String]('base64Encode)

    it("should return a base64 encoded string") {
      identityV3Handler invokePrivate base64Encode("""{"endpoints":["endpoint":{"id":"test-id","url":"http://test-url.com/test"}]}""") should fullyMatch regex "eyJlbmRwb2ludHMiOlsiZW5kcG9pbnQiOnsiaWQiOiJ0ZXN0LWlkIiwidXJsIjoiaHR0cDovL3Rlc3QtdXJsLmNvbS90ZXN0In1dfQ=="
    }
  }

  describe("isProjectIdValid") {
    val isProjectIdValid = PrivateMethod[Boolean]('isProjectIdValid)

    it("should return true if no valid project id in uri config element is present") {
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(new OpenstackIdentityService())
      config.getOpenstackIdentityService.setUri("")

      val handler = new OpenStackIdentityV3Handler(config, identityAPI)

      handler invokePrivate isProjectIdValid("", ValidToken(None, None, None, null, None, None, None, List(), null)) shouldBe true
    }

    it("should return false if no a project ID could not be extracted from the URI") {
      identityV3Handler invokePrivate isProjectIdValid("/foo/bar", ValidToken(None, None, None, null, None, None, None, List(), List())) shouldBe false
    }
  }

  describe("extractProjectIdFromUri") {
    val extractProjectIdFromUri = PrivateMethod[Option[String]]('extractProjectIdFromUri)

    it("should return None if the regex does not match") {
      identityV3Handler invokePrivate extractProjectIdFromUri("""/foo/(\d+)""".r, "/bar/12345") shouldBe None
    }

    it("should return None if the regex does not contain a capture group") {
      identityV3Handler invokePrivate extractProjectIdFromUri("""/foo/\d+""".r, "/bar/12345") shouldBe None
    }

    it("should return Some(projectId) if the regex matches and a capture group is present") {
      val projectId = identityV3Handler invokePrivate extractProjectIdFromUri("""/foo/(\d+)""".r, "/foo/12345")
      projectId shouldBe a[Some[_]]
      projectId.get shouldEqual "12345"
    }
  }

  describe("projectMatches") {
    val projectMatches = PrivateMethod[Boolean]('projectMatches)

    it("should return false if no project IDs match") {
      identityV3Handler invokePrivate projectMatches("12345", Some("09876"), List(Role("name", Some("09876")))) shouldBe false
    }

    it("should return true if the default project ID matches") {
      identityV3Handler invokePrivate projectMatches("12345", Some("12345"), List(Role("name", Some("09876")))) shouldBe true
    }

    it("should return true if a role project ID matches") {
      identityV3Handler invokePrivate projectMatches("12345", Some("09876"), List(Role("name", Some("12345")))) shouldBe true
    }

    it("should return true if the default project ID matches after stripping a prefix") {
      identityV3Handler invokePrivate projectMatches("12345", Some("foo:12345"), List(Role("name", Some("09876")))) shouldBe true
    }

    it("should return true if a role project ID matches after stripping a prefix") {
      identityV3Handler invokePrivate projectMatches("12345", Some("09876"), List(Role("name", Some("bar:12345")))) shouldBe true
    }
  }

  describe("isUserPreAuthed") {
    val isUserPreAuthed = PrivateMethod[Boolean]('isUserPreAuthed)

    it("should return true if the user had a role which bypasses validation") {
      identityV3Handler invokePrivate isUserPreAuthed(ValidToken(None, None, None, null, None, None, None, List(), List(Role("admin")))) shouldBe true
    }

    it("should return false if the user does not have a role which is in the bypass roles list") {
      identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("a")
      identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("b")
      identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("c")
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)

      identityV3Handler invokePrivate isUserPreAuthed(ValidToken(None, None, None, null, None, None, None, List(), List(Role("d"), Role("e")))) shouldBe false
    }

    it("should return true if the user does have a role which is in the bypass roles list") {
      identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("a")
      identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("b")
      identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("c")
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)

      identityV3Handler invokePrivate isUserPreAuthed(ValidToken(None, None, None, null, None, None, None, List(), List(Role("a"), Role("e")))) shouldBe true
    }
  }
}
