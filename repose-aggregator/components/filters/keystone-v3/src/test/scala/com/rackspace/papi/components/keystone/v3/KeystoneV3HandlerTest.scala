package com.rackspace.papi.components.keystone.v3

import java.io.ByteArrayInputStream
import javax.ws.rs.core.MediaType

import com.mockrunner.mock.web.MockHttpServletRequest
import com.rackspace.papi.commons.util.http.{HttpStatusCode, ServiceClientResponse}
import com.rackspace.papi.components.keystone.v3.config.{KeystoneV3Config, OpenstackKeystoneService}
import com.rackspace.papi.components.keystone.v3.objects.{AuthenticateResponse, EndpointType}
import com.rackspace.papi.components.keystone.v3.utilities.InvalidAdminCredentialsException
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.httpclient.{HttpClientResponse, HttpClientService}
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.apache.http.message.BasicHeader
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyMap, anyString, contains}
import org.mockito.Mockito
import org.mockito.Mockito.when
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

        when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
        when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)

        keystoneV3Handler = new KeystoneV3Handler(keystoneConfig, mockAkkaServiceClient, mockDatastoreService)
    }

    describe("handleRequest")(pending)

    describe("isUriWhitelisted") {
        val isUriWhitelisted = PrivateMethod[Boolean]('isUriWhitelisted)

        it("should return true if uri is in the whitelist") {
            val whiteList = List("/test1", "/test2")
            keystoneV3Handler invokePrivate isUriWhitelisted("/test1", whiteList) should be(true)
        }

        it("should return false if uri isn't in the whitelist") {
            val whiteList = List("/test1", "/test2")
            keystoneV3Handler invokePrivate isUriWhitelisted("/test3", whiteList) should be(false)
        }
    }

    describe("authenticate") {
        it("should return unauthorized when the x-auth-token header is not present") {
            val authenticate = PrivateMethod[FilterDirector]('authenticate)
            val mockRequest = new MockHttpServletRequest()

            val filterDirector = keystoneV3Handler invokePrivate authenticate(mockRequest)

            filterDirector.getResponseStatus should be(HttpStatusCode.UNAUTHORIZED)
            filterDirector.getFilterAction should be(FilterAction.RETURN)
        }
    }

    describe("validateSubjectToken") {
        val validateSubjectToken = PrivateMethod[Try[_]]('validateSubjectToken)

        it("should return a Failure when x-subject-token validation fails") {
            val mockPostServiceClientResponse = mock[ServiceClientResponse]
            val mockGetServiceClientResponse = mock[ServiceClientResponse]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockPostServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.CREATED.intValue)
            when(mockPostServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader("X-Subject-Token", "test-admin-token")), Nil: _*)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockPostServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            when(mockGetServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.NOT_FOUND.intValue)
            when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)

            keystoneV3Handler invokePrivate validateSubjectToken("test-subject-token") shouldBe a[Failure[_]]
        }

        it("should return a token object when x-subject-token validation succeeds") {
            val mockPostServiceClientResponse = mock[ServiceClientResponse]
            val mockGetServiceClientResponse = mock[ServiceClientResponse]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockPostServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.CREATED.intValue)
            when(mockPostServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader("X-Subject-Token", "test-admin-token")), Nil: _*)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockPostServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            when(mockGetServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.OK.intValue)
            when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
                "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
                        .getBytes))
            when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)

            keystoneV3Handler invokePrivate validateSubjectToken("test-subject-token") shouldBe a[Success[_]]
            keystoneV3Handler.invokePrivate(validateSubjectToken("test-subject-token")).get shouldBe an[AuthenticateResponse]
        }
    }

    describe("fetchAdminToken") {
        val fetchAdminToken = PrivateMethod[Try[String]]('fetchAdminToken)

        it("should build a JSON auth token request without a domain ID") {
            val mockServiceClientResponse = mock[ServiceClientResponse]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            keystoneV3Handler invokePrivate fetchAdminToken()

            Mockito.verify(mockAkkaServiceClient).post(
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

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")
            keystoneConfig.getKeystoneService.setDomainId("domainId")

            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)
            keystoneV3Handler invokePrivate fetchAdminToken()

            Mockito.verify(mockAkkaServiceClient).post(
                anyString,
                anyString,
                anyMap.asInstanceOf[java.util.Map[String, String]],
                contains("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"domain\":{\"id\":\"domainId\"},\"name\":\"user\",\"password\":\"password\"}}}}}"),
                any[MediaType],
                any[MediaType]
            )
        }

        it("should return a Failure when unable to retrieve admin token") {
            val mockServiceClientResponse = mock[ServiceClientResponse]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            keystoneV3Handler invokePrivate fetchAdminToken() shouldBe a[Failure[_]]
            keystoneV3Handler.invokePrivate(fetchAdminToken()).failed.get shouldBe a[InvalidAdminCredentialsException]
        }

        it("should return an admin token as a string when the admin API call succeeds") {
            val mockServiceClientResponse = mock[ServiceClientResponse]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.CREATED.intValue)
            when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader("X-Subject-Token", "test-admin-token")), Nil: _*)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            keystoneV3Handler invokePrivate fetchAdminToken() shouldBe a[Success[_]]
            keystoneV3Handler.invokePrivate(fetchAdminToken()).get should startWith("test-admin-token")
        }
    }

    describe("containsEndpoint") {
        val containsEndpoint = PrivateMethod[Boolean]('containsEndpoint)

        it("should return true when there is an endpoint that matches the url") {
            keystoneV3Handler invokePrivate containsEndpoint(List(EndpointType(null, null, null, null, "http://www.woot.com"), EndpointType(null, null, null, null, "http://www.notreallyawebsite.com")), "http://www.notreallyawebsite.com") should be(true)
        }

        it("should return false when there isn't an endpoint that matches the url") {
            keystoneV3Handler invokePrivate containsEndpoint(List(EndpointType(null, null, null, null, "http://www.woot.com"), EndpointType(null, null, null, null, "http://www.banana.com")), "http://www.notreallyawebsite.com") should be(false)
        }
    }
}
