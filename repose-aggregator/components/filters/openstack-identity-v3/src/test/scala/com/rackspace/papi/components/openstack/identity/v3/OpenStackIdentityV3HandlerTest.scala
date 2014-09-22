package com.rackspace.papi.components.openstack.identity.v3

import com.mockrunner.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import com.rackspace.papi.commons.util.http.header.HeaderName
import com.rackspace.papi.commons.util.http.{CommonHttpHeader, HttpStatusCode}
import com.rackspace.papi.commons.util.servlet.http.{MutableHttpServletResponse, ReadableHttpServletResponse}
import com.rackspace.papi.components.openstack.identity.v3.config._
import com.rackspace.papi.components.openstack.identity.v3.objects._
import com.rackspace.papi.components.openstack.identity.v3.utilities._
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector, HeaderManager}
import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, when}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

import scala.util.matching.Regex
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
      identityV3Handler.handleRequest(mockRequest, mockServletResponse).getResponseStatus equals HttpStatusCode.UNAUTHORIZED
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
          responseStatus -> HttpStatusCode.OK,
          resultStatus -> HttpStatusCode.OK
        ),
        Map(
          responseStatus -> HttpStatusCode.FORBIDDEN,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> HttpStatusCode.FORBIDDEN,
          resultWwwAuthenticate -> "Keystone uri=http://test-uri.com"
        ),
        Map(
          responseStatus -> HttpStatusCode.UNAUTHORIZED,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> HttpStatusCode.FORBIDDEN,
          resultWwwAuthenticate -> "Keystone uri=http://test-uri.com"
        ),
        Map(
          responseStatus -> HttpStatusCode.UNAUTHORIZED,
          resultStatus -> HttpStatusCode.INTERNAL_SERVER_ERROR
        ),
        Map(
          responseStatus -> HttpStatusCode.NOT_IMPLEMENTED,
          responseWwwAuthenticate -> OpenStackIdentityV3Headers.X_DELEGATED,
          resultStatus -> HttpStatusCode.INTERNAL_SERVER_ERROR
        ),
        Map(
          responseStatus -> HttpStatusCode.NOT_IMPLEMENTED,
          resultStatus -> HttpStatusCode.NOT_IMPLEMENTED
        )
      ).map { parameterMap =>
        mockServletResponse.setStatus(parameterMap.get(responseStatus).get.asInstanceOf[HttpStatusCode].intValue)
        if (parameterMap.get(responseWwwAuthenticate).isDefined) {
          mockServletResponse.addHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, parameterMap.get(responseWwwAuthenticate).get.asInstanceOf[String])
        }

        val responseFilterDirector = identityV3Handler.handleResponse(mockServletRequest, MutableHttpServletResponse.wrap(mockServletRequest, mockServletResponse))

        responseFilterDirector.getResponseStatus shouldBe parameterMap.get(resultStatus).get
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
    val filterDirector = mock[FilterDirector]
    val headerManager = mock[HeaderManager]
    when(filterDirector.requestHeaderManager()).thenReturn(headerManager)
    val roles = List(Role(null, null, Option("12345"), null, null), Role(null, null, Option("67890"), null, null))

    it("should only provide the url project when the flag says to not write all") {
      identityV3Handler invokePrivate writeProjectHeader("abcde", roles, false, filterDirector)
      verify(headerManager).appendHeader(org.mockito.Matchers.eq("X-PROJECT-ID"), org.mockito.Matchers.eq("abcde"))
    }

    it("should provide all the projects when the flag says to write all") {
      identityV3Handler invokePrivate writeProjectHeader("abcde", roles, true, filterDirector)
      verify(headerManager).appendHeader(org.mockito.Matchers.eq("X-PROJECT-ID"), org.mockito.Matchers.eq("12345"), org.mockito.Matchers.eq("67890"), org.mockito.Matchers.eq("abcde"))
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

    it("should return true if no validate project id in uri config element is present") {
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(new OpenstackIdentityService())
      config.getOpenstackIdentityService.setUri("")

      val handler = new OpenStackIdentityV3Handler(config, identityAPI)

      handler invokePrivate isProjectIdValid("", AuthenticateResponse(null, null, null, null, null, null, null, null)) shouldBe true
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
      projectId shouldBe a [Some[_]]
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
}
