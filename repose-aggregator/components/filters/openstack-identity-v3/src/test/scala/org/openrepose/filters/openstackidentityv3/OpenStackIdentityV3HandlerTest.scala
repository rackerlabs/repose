package org.openrepose.filters.openstackidentityv3

import java.util
import javax.servlet.http.HttpServletResponse

import com.mockrunner.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mockitoEq}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletResponse, ReadableHttpServletResponse}
import org.openrepose.core.filter.logic.{FilterAction, HeaderManager}
import org.openrepose.filters.openstackidentityv3.config._
import org.openrepose.filters.openstackidentityv3.objects._
import org.openrepose.filters.openstackidentityv3.utilities._
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConversions
import scala.util.{Failure, Try}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3HandlerTest extends FunSpec with BeforeAndAfter with Matchers with PrivateMethodTester with MockitoSugar {

  var identityV3Handler: OpenStackIdentityV3Handler = _
  var identityConfig: OpenstackIdentityV3Config = _
  var identityAPI: OpenStackIdentityV3API = _

  before {
    identityConfig = new OpenstackIdentityV3Config()
    identityConfig.setOpenstackIdentityService(new OpenstackIdentityService())
    identityConfig.getOpenstackIdentityService.setUsername("user")
    identityConfig.getOpenstackIdentityService.setPassword("password")
    identityConfig.getOpenstackIdentityService.setUri("http://test-uri.com")
    identityConfig.setServiceEndpoint(new ServiceEndpoint())
    identityConfig.getServiceEndpoint.setUrl("http://www.notreallyawebsite.com")
    identityConfig.setValidateProjectIdInUri(new ValidateProjectID())
    identityConfig.getValidateProjectIdInUri.setRegex("""/foo/(\d+)""")
    identityConfig.setRolesWhichBypassProjectIdCheck(new IgnoreProjectIDRoles())
    identityConfig.getRolesWhichBypassProjectIdCheck.getRole.add("admin")
    identityConfig.setForwardGroups(false)
    identityAPI = mock[OpenStackIdentityV3API]

    identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
  }

  describe("handleRequest") {
    val mockServletResponse = mock[ReadableHttpServletResponse]

    it("should pass filter if uri is in the whitelist") {
      val whiteList = new WhiteList()
      whiteList.getUriPattern.add("/test1")
      whiteList.getUriPattern.add("/test2")
      identityConfig.setWhiteList(whiteList)

      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")

      identityV3Handler.handleRequest(mockRequest, mockServletResponse).getFilterAction equals FilterAction.PASS
    }

    it("should attempt validation if uri isn't in the whitelist") {
      val whiteList = new WhiteList()
      whiteList.getUriPattern.add("/test1")
      whiteList.getUriPattern.add("/test2")
      identityConfig.setWhiteList(whiteList)

      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test3")

      identityV3Handler.handleRequest(mockRequest, mockServletResponse).getFilterAction equals FilterAction.RETURN
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).getResponseStatusCode equals HttpServletResponse.SC_UNAUTHORIZED
    }

    it("should add the X-Default-Region if rax_default_region is available for the user") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(new AuthenticateResponse("1", "2", List(), None, None, Option(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Option(List(Role("1", "admin"))), UserForAuthenticateResponse(null, None, None, None, None, Some("ORD")))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should contain(
        Entry(
          HeaderName.wrap("X-Default-Region"),
          JavaConversions.setAsJavaSet(Set("ORD")))
      )
    }

    it("should not include X-Default-Region if rax_default_region is not available for the user") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(new AuthenticateResponse("1", "2", List(), None, None, Option(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Option(List(Role("1", "admin"))), new UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should not contain key(HeaderName.wrap("X-Default-Region"))
    }

    it("should add the X-Impersonator-Name and X-Impersonator-ID headers if the impersonator's information is available") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(new AuthenticateResponse("1", "2", List(), None, None, Option(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Option(List(Role("1", "admin"))), new UserForAuthenticateResponse(null), Some(new ImpersonatorForAuthenticationResponse(Some("ImpersonationId"), Some("ImpersonationName"))))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      val headers: util.Map[HeaderName, util.Set[String]] = identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd
      headers should contain(
        Entry(
          HeaderName.wrap("X-Impersonator-Name"),
          JavaConversions.setAsJavaSet(Set("ImpersonationName")))
      )
      headers should contain(
        Entry(
          HeaderName.wrap("X-Impersonator-Id"),
          JavaConversions.setAsJavaSet(Set("ImpersonationId")))
      )
    }

    it("should not add the X-Impersonator-Name and X-Impersonator-ID headers if the impersonator's information is not available") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(new AuthenticateResponse("1", "2", List(), None, None, Option(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Option(List(Role("1", "admin"))), new UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      identityConfig.setForwardGroups(false)
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      val headers: util.Map[HeaderName, util.Set[String]] = identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd
      headers.keySet() should not contain HeaderName.wrap("X-Impersonator-Name")
      headers.keySet() should not contain HeaderName.wrap("X-Impersonator-Id")
    }

    it("should set the x-project-id header to the uri project id value if it is set and send all project ids is not set/false") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(AuthenticateResponse("1", "2", List(), None,
          Some(ProjectForAuthenticateResponse(null, Some("ProjectIdToNotSee"))),
          Some(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Some(List(Role("1", "admin", Some("ProjectToNotSee")))), UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/12345")
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should contain(
        Entry(
          HeaderName.wrap("X-Project-Id"),
          JavaConversions.setAsJavaSet(Set("12345")))
      )
    }

    it("should set the x-project-id header to the default project id if send all project ids is not set/false and there is no uri project id validation") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(AuthenticateResponse("1", "2", List(), None,
          Some(ProjectForAuthenticateResponse(null, Some("DefaultProjectIdToSee"))),
          Some(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Some(List(Role("1", "admin", Some("ProjectToNotSee")))), UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/bar")
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should contain(
        Entry(
          HeaderName.wrap("X-Project-Id"),
          JavaConversions.setAsJavaSet(Set("DefaultProjectIdToSee")))
      )
    }

    it("should not set the x-project-id header if there is no default and send all project ids is not set/false and there is no uri project id validation") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(AuthenticateResponse("1", "2", List(), None, None,
          Some(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Some(List(Role("1", "admin", Some("ProjectToNotSee")))), UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/bar")
      identityConfig.setValidateProjectIdInUri(null)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should not contain key(HeaderName.wrap("X-Project-Id"))
    }

    it("should return default project id and roles project ids returned by identity as multiple x-project-id headers if all project ids is true and there is no uri project id validation") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(AuthenticateResponse("1", "2", List(), None,
          Some(ProjectForAuthenticateResponse(null, Some("ProjectIdFromProject"))),
          Some(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Some(List(Role("1", "admin", Some("ProjectIdFromRoles")))), UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/12345")
      identityConfig.setSendAllProjectIds(true)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should contain(
        Entry(
          HeaderName.wrap("X-Project-Id"),
          JavaConversions.setAsJavaSet(Set("ProjectIdFromProject", "ProjectIdFromRoles")))
      )
    }

    it("should return all project ids returned by identity as multiple x-project-id headers if all project ids is true") {
      when(identityAPI.validateToken("123456")).thenReturn(
        Try(AuthenticateResponse("1", "2", List(), None,
          Some(ProjectForAuthenticateResponse(null, Some("ProjectIdFromProject"))),
          Some(List(ServiceForAuthenticationResponse(List(Endpoint("foo", None, None, None, "http://www.notreallyawebsite.com"))))),
          Some(List(Role("1", "admin", Some("ProjectIdFromRoles"), Some("RaxExtensionProjectId")))), UserForAuthenticateResponse(null))))
      val mockRequest = new MockHttpServletRequest()
      mockRequest.setHeader("X-Subject-Token", "123456")
      mockRequest.setRequestURI("/foo/12345")
      identityConfig.setSendAllProjectIds(true)
      identityV3Handler = new OpenStackIdentityV3Handler(identityConfig, identityAPI)
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).requestHeaderManager.headersToAdd should contain(
        Entry(
          HeaderName.wrap("X-Project-Id"),
          JavaConversions.setAsJavaSet(Set("ProjectIdFromProject", "ProjectIdFromRoles", "RaxExtensionProjectId")))
      )
    }
  }

  describe("handleResponse") {
    // TODO: Get this to work, or make it a system level test
    ignore("should set the appropriate response status") {
      val mockServletRequest = new MockHttpServletRequest()
      val mockServletResponse = new MockHttpServletResponse()

      val responseStatus = "response-status-key"
      val responseWwwAuthenticate = "response-www-authenticate"
      val resultStatus = "result-status"
      val resultWwwAuthenticate = "result-www-authenticate"

      List(
        Map(
          responseStatus -> HttpServletResponse.SC_OK,
          resultStatus -> HttpServletResponse.SC_OK
        ),
        Map(
          responseStatus -> HttpServletResponse.SC_FORBIDDEN,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> HttpServletResponse.SC_FORBIDDEN,
          resultWwwAuthenticate -> "Keystone uri=http://test-uri.com"
        ),
        Map(
          responseStatus -> HttpServletResponse.SC_UNAUTHORIZED,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> HttpServletResponse.SC_FORBIDDEN,
          resultWwwAuthenticate -> "Keystone uri=http://test-uri.com"
        ),
        Map(
          responseStatus -> HttpServletResponse.SC_UNAUTHORIZED,
          resultStatus -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        ),
        Map(
          responseStatus -> HttpServletResponse.SC_NOT_IMPLEMENTED,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        ),
        Map(
          responseStatus -> HttpServletResponse.SC_NOT_IMPLEMENTED,
          resultStatus -> HttpServletResponse.SC_NOT_IMPLEMENTED
        )
      ).map { parameterMap =>
        mockServletResponse.setStatus(parameterMap.get(responseStatus).get.asInstanceOf[Integer])
        if (parameterMap.get(responseWwwAuthenticate).isDefined) {
          mockServletResponse.addHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, parameterMap.get(responseWwwAuthenticate).get.asInstanceOf[String])
        }

        val responseFilterDirector = identityV3Handler.handleResponse(mockServletRequest, MutableHttpServletResponse.wrap(mockServletRequest, mockServletResponse))

        responseFilterDirector.getResponseStatusCode shouldBe parameterMap.get(resultStatus).get
        if (parameterMap.get(resultWwwAuthenticate).isDefined) {
          responseFilterDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap(CommonHttpHeader.WWW_AUTHENTICATE.toString)) should contain(parameterMap.get(resultWwwAuthenticate).get)
        }
      }
    }
  }

  describe("authenticate") {
    val authenticate = PrivateMethod[Try[AuthenticateResponse]]('authenticate)

    it("should return a Failure when the x-subject-token header is not present") {
      val mockRequest = new MockHttpServletRequest()

      identityV3Handler invokePrivate authenticate(mockRequest) shouldBe a[Failure[_]]
      an[InvalidSubjectTokenException] should be thrownBy identityV3Handler.invokePrivate(authenticate(mockRequest)).get
    }
  }

  describe("writeProjectHeader") {
    val writeProjectHeader = PrivateMethod[Unit]('writeProjectHeader)
    val headerManager = mock[HeaderManager]
    val roles = List(Role(null, null, Option("12345"), null, null), Role(null, null, Option("67890"), null, null))

    it("should only provide the url project when the flag says to not write all") {
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), false, false, headerManager)
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq(uriPid))
    }

    it("should provide all the projects when the flag says to write all") {
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), true, false, headerManager)
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid))
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq("12345"))
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq("67890"))
    }

    it("should handle when roles are present but project ids are missing") {
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      val roleList = List(Role(null, null, None, None, None), Role(null, null, None, None, None))
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roleList, Some(uriPid), true, false, headerManager)
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid))
    }

    it("should handle when project ids are missing and not missing") {
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      val roleList = List(Role(null, null, None, None, None), Role(null, null, Option("foo"), None, None), Role(null, null, None, None, None))
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roleList, Some(uriPid), true, false, headerManager)
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid))
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq("foo"))
    }

    it("should add qualities when flagged and not writing all") {
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), false, true, headerManager)
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq(uriPid), mockitoEq(1.0))
    }

    it("should add qualities when flagged and writing all") {
      val defaultPid = java.util.UUID.randomUUID.toString
      val uriPid = java.util.UUID.randomUUID.toString
      identityV3Handler invokePrivate writeProjectHeader(Some(defaultPid), roles, Some(uriPid), true, true, headerManager)
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq(defaultPid), mockitoEq(1.0))
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq("12345"), mockitoEq(0.5))
      verify(headerManager).appendHeader(mockitoEq("X-Project-ID"), mockitoEq("67890"), mockitoEq(0.5))
    }
  }

  describe("containsRequiredEndpoint") {
    val containsRequiredEndpoint = PrivateMethod[Boolean]('containsRequiredEndpoint)

    it("should return true when there is an endpoint that matches the url") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, None, None, None, "http://www.notreallyawebsite.com")),
        Endpoint(null, None, None, None, "http://www.notreallyawebsite.com")
      ) should be(true)
    }

    it("should return false when there isn't an endpoint that matches the url") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, None, None, None, "http://www.banana.com")),
        Endpoint(null, None, None, None, "http://www.notreallyawebsite.com")
      ) should be(false)
    }

    it("Should return true when the url matches and region does") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, None, null, Option("DFW"), "http://www.notreallyawebsite.com")),
        Endpoint(null, None, None, Option("DFW"), "http://www.notreallyawebsite.com")
      ) should be(true)
    }

    it("Should return false when the url matches and region doesn't") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, None, None, None, "http://www.notreallyawebsite.com")),
        Endpoint(null, None, None, Option("DFW"), "http://www.notreallyawebsite.com")
      ) should be(false)
    }

    it("Should return true when the url matches and name does") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, Option("foo"), None, Option("DFW"), "http://www.notreallyawebsite.com")),
        Endpoint(null, Option("foo"), None, None, "http://www.notreallyawebsite.com")
      ) should be(true)
    }

    it("Should return false when the url matches and name doesn't") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, Option("bar"), null, None, "http://www.notreallyawebsite.com")),
        Endpoint(null, Option("foo"), None, None, "http://www.notreallyawebsite.com")
      ) should be(false)
    }

    it("Should return true when the url matches and interface does") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, Option("foo"), Option("foo"), Option("DFW"), "http://www.notreallyawebsite.com")),
        Endpoint(null, None, Option("foo"), None, "http://www.notreallyawebsite.com")
      ) should be(true)
    }

    it("Should return false when the url matches and interface doesn't") {
      identityV3Handler invokePrivate containsRequiredEndpoint(
        List(Endpoint(null, None, None, None, "http://www.woot.com"), Endpoint(null, Option("bar"), None, None, "http://www.notreallyawebsite.com")),
        Endpoint(null, None, Option("foo"), None, "http://www.notreallyawebsite.com")
      ) should be(false)
    }
  }

  describe("isAuthorized") {
    val isAuthorized = PrivateMethod[Boolean]('isAuthorized)

    it("should return true when not configured to check endpoints") {
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(new OpenstackIdentityService())
      config.getOpenstackIdentityService.setUri("")

      val handler = new OpenStackIdentityV3Handler(config, identityAPI)

      handler invokePrivate isAuthorized(AuthenticateResponse(null, null, null, null, null, null, null, null)) should be(true)
    }

    it("should return true when configured and the endpoint is present") {
      val catalog = List(ServiceForAuthenticationResponse(List(Endpoint(null, None, None, None, "http://www.notreallyawebsite.com")), null, null))
      val authToken = AuthenticateResponse(null, null, null, null, null, Option(catalog), null, null)

      identityV3Handler invokePrivate isAuthorized(authToken) should be(true)
    }

    it("should return false when configured and the endpoint is not present") {
      val catalog = List(ServiceForAuthenticationResponse(List(Endpoint(null, None, None, None, "http://www.woot.com")), null, null))
      val authToken = AuthenticateResponse(null, null, null, null, null, Option(catalog), null, null)

      identityV3Handler invokePrivate isAuthorized(authToken) should be(false)
    }
  }

  describe("base64Encode") {
    val base64Encode = PrivateMethod[String]('base64Encode)

    it("should return a base64 encoded string") {
      identityV3Handler invokePrivate base64Encode("{\"endpoints\":[\"endpoint\":{\"id\":\"test-id\",\"url\":\"http://test-url.com/test\"}]}") should fullyMatch regex "eyJlbmRwb2ludHMiOlsiZW5kcG9pbnQiOnsiaWQiOiJ0ZXN0LWlkIiwidXJsIjoiaHR0cDovL3Rlc3QtdXJsLmNvbS90ZXN0In1dfQ=="
    }
  }

  describe("isProjectIdValid") {
    val isProjectIdValid = PrivateMethod[Boolean]('isProjectIdValid)

    it("should return true if no valid project id in uri config element is present") {
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(new OpenstackIdentityService())
      config.getOpenstackIdentityService.setUri("")

      val handler = new OpenStackIdentityV3Handler(config, identityAPI)

      handler invokePrivate isProjectIdValid("", AuthenticateResponse(null, null, null, null, null, null, null, null)) shouldBe true
    }

    it("should return true if the user had a role which bypasses validation") {
      identityV3Handler invokePrivate isProjectIdValid("", AuthenticateResponse(null, null, null, None, None, None, Some(List(Role("12345", "admin"))), null)) shouldBe true
    }

    it("should return false if no a project ID could not be extracted from the URI") {
      identityV3Handler invokePrivate isProjectIdValid("/foo/bar", AuthenticateResponse(null, null, null, None, None, None, None, null)) shouldBe false
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
      identityV3Handler invokePrivate projectMatches("12345", Some("09876"), List(Role("id", "name", Some("09876")))) shouldBe false
    }

    it("should return true if the default project ID matches") {
      identityV3Handler invokePrivate projectMatches("12345", Some("12345"), List(Role("id", "name", Some("09876")))) shouldBe true
    }

    it("should return true if a role project ID matches") {
      identityV3Handler invokePrivate projectMatches("12345", Some("09876"), List(Role("id", "name", Some("12345")))) shouldBe true
    }
  }

  describe("hasIgnoreEnabledRole") {
    val hasIgnoreEnabledRole = PrivateMethod[Boolean]('hasIgnoreEnabledRole)

    it("should return false if the user does not have a role which is in the bypass roles list") {
      val ignoreRoles = List("a", "b", "c")
      val userRoles = List("d", "e")

      identityV3Handler invokePrivate hasIgnoreEnabledRole(ignoreRoles, userRoles) shouldBe false
    }

    it("should return true if the user does have a role which is in the bypass roles list") {
      val ignoreRoles = List("a", "b", "c")
      val userRoles = List("a", "e")

      identityV3Handler invokePrivate hasIgnoreEnabledRole(ignoreRoles, userRoles) shouldBe true
    }
  }
}
