package com.rackspace.papi.components.keystone.v3

import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType

import com.mockrunner.mock.web.MockHttpServletRequest
import com.rackspace.papi.commons.util.http.{HttpStatusCode, ServiceClientResponse}
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.datastore.Datastore
import com.rackspace.papi.components.keystone.v3.config.{KeystoneV3Config, OpenstackKeystoneService, WhiteList}
import com.rackspace.papi.components.keystone.v3.objects._
import com.rackspace.papi.components.keystone.v3.utilities.KeystoneV3Headers
import com.rackspace.papi.components.keystone.v3.utilities.exceptions.InvalidAdminCredentialsException
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector, HeaderManager}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.apache.http.message.BasicHeader
import org.hamcrest.Matchers.{equalTo, lessThanOrEqualTo}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyInt, anyMap, anyString, argThat, contains, intThat}
import org.mockito.Mockito.{verify, when}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class KeystoneV3HandlerTest extends FunSpec with BeforeAndAfter with Matchers with PrivateMethodTester with MockitoSugar {

  var keystoneV3Handler: KeystoneV3Handler = _
  var keystoneConfig: KeystoneV3Config = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockDatastoreService: DatastoreService = _
  var mockDatastore: Datastore = _

  before {
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockDatastoreService = mock[DatastoreService]
    mockDatastore = mock[Datastore]
    keystoneConfig = new KeystoneV3Config()
    keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
    keystoneConfig.getKeystoneService.setUsername("user")
    keystoneConfig.getKeystoneService.setPassword("password")
    keystoneConfig.getKeystoneService.setUri("http://test-uri.com")

    when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)

    keystoneV3Handler = new KeystoneV3Handler(keystoneConfig, mockAkkaServiceClient, mockDatastoreService)
  }

  describe("handleRequest") {
    val mockServletResponse = mock[ReadableHttpServletResponse]

    it("should pass filter if uri is in the whitelist") {
      val whiteList = new WhiteList()
      whiteList.getUriPattern.add("/test1")
      whiteList.getUriPattern.add("/test2")
      keystoneConfig.setWhiteList(whiteList)

      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test1")

      keystoneV3Handler.handleRequest(mockRequest, mockServletResponse).getFilterAction should be theSameInstanceAs FilterAction.PASS
    }

    it("should attempt validation if uri isn't in the whitelist") {
      val whiteList = new WhiteList()
      whiteList.getUriPattern.add("/test1")
      whiteList.getUriPattern.add("/test2")
      keystoneConfig.setWhiteList(whiteList)

      val mockRequest = new MockHttpServletRequest()
      mockRequest.setRequestURI("/test3")

      keystoneV3Handler.handleRequest(mockRequest, mockServletResponse).getFilterAction should be theSameInstanceAs FilterAction.RETURN
      keystoneV3Handler.handleRequest(mockRequest, mockServletResponse).getResponseStatus should be theSameInstanceAs HttpStatusCode.UNAUTHORIZED
    }
  }

  describe("authenticate") {
    val authenticate = PrivateMethod[FilterDirector]('authenticate)

    it("should return unauthorized when the x-auth-token header is not present") {
      val mockRequest = new MockHttpServletRequest()

      val filterDirector = keystoneV3Handler invokePrivate authenticate(mockRequest)

      filterDirector.getResponseStatus should be(HttpStatusCode.UNAUTHORIZED)
      filterDirector.getFilterAction should be(FilterAction.RETURN)
    }

    it("should set headers if request passes through filter") {
      val mockRequest = new MockHttpServletRequest()
      mockRequest.addHeader(KeystoneV3Headers.X_SUBJECT_TOKEN, "test-subject-token")
    }

    it("should set X-Authorization and X-Identity-Status headers when configured to forward all requests")(pending)
  }

  describe("validateSubjectToken") {
    val validateSubjectToken = PrivateMethod[Try[_]]('validateSubjectToken)

    it("should return a Failure when x-subject-token validation fails") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockDatastore.get(contains("ADMIN_TOKEN"))).thenReturn("test-admin-token", Nil: _*)
      when(mockGetServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.NOT_FOUND.intValue)
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)

      keystoneV3Handler invokePrivate validateSubjectToken("test-subject-token", false) shouldBe a[Failure[_]]
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn(AuthenticateResponse(null, null), Nil: _*)

      keystoneV3Handler invokePrivate validateSubjectToken("test-subject-token", false) shouldBe a[Success[_]]
      keystoneV3Handler.invokePrivate(validateSubjectToken("test-subject-token", false)).get shouldBe an[AuthenticateResponse]
    }

    it("should return a token object when x-subject-token validation succeeds") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockDatastore.get(contains("ADMIN_TOKEN"))).thenReturn("test-admin-token", Nil: _*)
      when(mockGetServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.OK.intValue)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)

      keystoneV3Handler invokePrivate validateSubjectToken("test-subject-token", false) shouldBe a[Success[_]]
      keystoneV3Handler.invokePrivate(validateSubjectToken("test-subject-token", false)).get shouldBe an[AuthenticateResponse]
    }

    it("should cache a token object when x-subject-token validation succeeds with the correct TTL") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]
      val currentTime = DateTime.now()
      val expirationTime = currentTime.plusMillis(100000)
      val returnJson = "{\"token\":{\"expires_at\":\"" + ISODateTimeFormat.dateTime().print(expirationTime) + "\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"

      when(mockDatastore.get(argThat(equalTo("ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)
      when(mockGetServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.OK.intValue)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(returnJson.getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)

      keystoneV3Handler invokePrivate validateSubjectToken("test-subject-token", false)

      verify(mockDatastore).put(argThat(equalTo("TOKEN:test-subject-token")), any[Serializable], intThat(lessThanOrEqualTo((expirationTime.getMillis - currentTime.getMillis).toInt)), any[TimeUnit])
    }
  }

  describe("fetchAdminToken") {
    val fetchAdminToken = PrivateMethod[Try[String]]('fetchAdminToken)

    it("should build a JSON auth token request without a domain ID") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      keystoneV3Handler invokePrivate fetchAdminToken(false)

      verify(mockAkkaServiceClient).post(
        anyString,
        anyString,
        anyMap.asInstanceOf[java.util.Map[String, String]],
        contains("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}"),
        any[MediaType],
        any[MediaType]
      )
    }

    it("should build a JSON auth token request with a string domain ID") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      keystoneConfig.getKeystoneService.setProjectId("projectId")

      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures
      when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)

      keystoneV3Handler invokePrivate fetchAdminToken(false)

      verify(mockAkkaServiceClient).post(
        anyString,
        anyString,
        anyMap.asInstanceOf[java.util.Map[String, String]],
        contains("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}},\"scope\":{\"project\":{\"id\":\"projectId\"}}}}"),
        any[MediaType],
        any[MediaType]
      )
    }

    it("should return a Failure when unable to retrieve admin token") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      keystoneV3Handler invokePrivate fetchAdminToken(false) shouldBe a[Failure[_]]
      keystoneV3Handler.invokePrivate(fetchAdminToken(false)).failed.get shouldBe a[InvalidAdminCredentialsException]
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn("test-cached-token", Nil: _*)

      keystoneV3Handler invokePrivate fetchAdminToken(false) shouldBe a[Success[_]]
      keystoneV3Handler.invokePrivate(fetchAdminToken(false)).get should startWith("test-cached-token")
    }

    it("should return an admin token as a string when the admin API call succeeds") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.CREATED.intValue)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(KeystoneV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      keystoneV3Handler invokePrivate fetchAdminToken(false) shouldBe a[Success[_]]
      keystoneV3Handler.invokePrivate(fetchAdminToken(false)).get should startWith("test-admin-token")
    }

    it("should return a new admin token (non-cached) if force is set to true") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockDatastore.get(anyString)).thenReturn("test-cached-token", Nil: _*)
      when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.CREATED.intValue)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(KeystoneV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      keystoneV3Handler invokePrivate fetchAdminToken(true) shouldBe a[Success[_]]
      keystoneV3Handler.invokePrivate(fetchAdminToken(true)).get should startWith("test-admin-token")
    }

    it("should cache an admin token when the admin API call succeeds") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.CREATED.intValue)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(KeystoneV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      keystoneV3Handler invokePrivate fetchAdminToken(false)

      verify(mockDatastore).put(contains("ADMIN_TOKEN"), any[Serializable], anyInt, any[TimeUnit])
    }
  }

  describe("writeProjectHeader") {
    val writeProjectHeader = PrivateMethod[Unit]('writeProjectHeader)
    val filterDirector = mock[FilterDirector]
    val headerManager = mock[HeaderManager]
    when(filterDirector.requestHeaderManager()).thenReturn(headerManager)
    val roles = List(Role(null, null, Option("12345"), null, null), Role(null, null, Option("67890"), null, null))

    it("should only provide the url project when the flag says to not write all") {
      keystoneV3Handler invokePrivate writeProjectHeader("abcde", roles, false, filterDirector)
      verify(headerManager).appendHeader(org.mockito.Matchers.eq("X-PROJECT-ID"), org.mockito.Matchers.eq("abcde"))
    }

    it("should provide all the projects when the flag says to write all") {
      keystoneV3Handler invokePrivate writeProjectHeader("abcde", roles, true, filterDirector)
      verify(headerManager).appendHeader(org.mockito.Matchers.eq("X-PROJECT-ID"), org.mockito.Matchers.eq("12345"), org.mockito.Matchers.eq("67890"), org.mockito.Matchers.eq("abcde"))
    }
  }

  describe("containsEndpoint") {
    val containsEndpoint = PrivateMethod[Boolean]('containsEndpoint)

    it("should return true when there is an endpoint that matches the url") {
      keystoneV3Handler invokePrivate containsEndpoint(List(Endpoint(null, null, null, null, "http://www.woot.com"), Endpoint(null, null, null, null, "http://www.notreallyawebsite.com")), "http://www.notreallyawebsite.com") should be(true)
    }

    it("should return false when there isn't an endpoint that matches the url") {
      keystoneV3Handler invokePrivate containsEndpoint(List(Endpoint(null, null, null, null, "http://www.woot.com"), Endpoint(null, null, null, null, "http://www.banana.com")), "http://www.notreallyawebsite.com") should be(false)
    }
  }
}
