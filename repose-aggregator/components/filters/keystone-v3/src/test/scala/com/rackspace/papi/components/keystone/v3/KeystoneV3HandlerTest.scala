package com.rackspace.papi.components.keystone.v3

import javax.ws.rs.core.MediaType

import com.mockrunner.mock.web.MockHttpServletRequest
import com.rackspace.papi.commons.util.http.{HttpStatusCode, ServiceClientResponse}
import com.rackspace.papi.components.keystone.v3.config.{KeystoneV3Config, OpenstackKeystoneService}
import com.rackspace.papi.components.keystone.v3.utilities.KeystoneAuthException
import com.rackspace.papi.commons.util.http.HttpStatusCode
import com.rackspace.papi.components.keystone.v3.config.{OpenstackKeystoneService, KeystoneV3Config}
import com.rackspace.papi.components.keystone.v3.objects.EndpointType
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.httpclient.{HttpClientResponse, HttpClientService}
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.apache.http.Header
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyMap, anyString}
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
    var mockConnectionPoolService: HttpClientService[_, _ <: HttpClientResponse] = _

    before {
        mockAkkaServiceClient = mock[AkkaServiceClient]
        mockDatastoreService = mock[DatastoreService]
        mockConnectionPoolService = mock[HttpClientService[_, _ <: HttpClientResponse]]
        keystoneConfig = new KeystoneV3Config()
        keystoneV3Handler = new KeystoneV3Handler(keystoneConfig, mockAkkaServiceClient, mockDatastoreService, mockConnectionPoolService)
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
        it("should return None when x-subject-token validation fails")(pending)

        it("should return token object when x-subject-token validation succeeds")(pending)
    }

    describe("fetchAdminToken") {
        val fetchAdminToken = PrivateMethod[Try[String]]('fetchAdminToken)

        it("should return a Failure when unable to retrieve admin token") {
            val mockServiceClientResponse = mock[ServiceClientResponse[Object]]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.UNAUTHORIZED.intValue)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            keystoneV3Handler invokePrivate fetchAdminToken() shouldBe a[Failure[_]]
            keystoneV3Handler.invokePrivate(fetchAdminToken()).failed.get shouldBe a[KeystoneAuthException]
        }

        it("should return an admin token as a string when the admin API call succeeds") {
            val mockServiceClientResponse = mock[ServiceClientResponse[Object]]
            val mockHeader = mock[Header]

            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            when(mockHeader.getName).thenReturn("X-Subject-Token", Nil: _*)
            when(mockHeader.getValue).thenReturn("test-admin-token", Nil: _*)
            when(mockServiceClientResponse.getStatusCode).thenReturn(HttpStatusCode.OK.intValue)
            when(mockServiceClientResponse.getHeaders).thenReturn(Array(mockHeader), Nil: _*)
            when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]), any(classOf[MediaType]))).
                    thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

            keystoneV3Handler invokePrivate fetchAdminToken() shouldBe a[Success[_]]
            keystoneV3Handler.invokePrivate(fetchAdminToken()).get should startWith("test-admin-token")
        }
    }

    describe("createAdminAuthRequest") {
        val createAdminAuthRequest = PrivateMethod[String]('createAdminAuthRequest)

        it("should build a JSON auth token request without a domain ID") {
            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")

            keystoneV3Handler invokePrivate createAdminAuthRequest() should equal("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}")
        }

        it("should build a JSON auth token request with a string domain ID") {
            keystoneConfig.setKeystoneService(new OpenstackKeystoneService())
            keystoneConfig.getKeystoneService.setUsername("user")
            keystoneConfig.getKeystoneService.setPassword("password")
            keystoneConfig.getKeystoneService.setDomainId("domainId")

            keystoneV3Handler invokePrivate createAdminAuthRequest() should equal("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"domain\":{\"id\":\"domainId\"},\"name\":\"user\",\"password\":\"password\"}}}}}")
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
