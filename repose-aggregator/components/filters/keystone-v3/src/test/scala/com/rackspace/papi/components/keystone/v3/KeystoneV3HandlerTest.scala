package com.rackspace.papi.components.keystone.v3

import com.mockrunner.mock.web.MockHttpServletRequest
import com.rackspace.papi.commons.util.http.HttpStatusCode
import com.rackspace.papi.components.keystone.v3.config.{OpenstackKeystoneService, KeystoneV3Config}
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.httpclient.HttpClientService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

@RunWith(classOf[JUnitRunner])
class KeystoneV3HandlerTest extends FunSpec with BeforeAndAfter with Matchers with PrivateMethodTester with MockitoSugar {

    var keystoneV3Handler: KeystoneV3Handler = _

    val mockAkkaServiceClient = mock[AkkaServiceClient]
    val mockDatastoreService = mock[DatastoreService]
    val mockConnectionPoolService = mock[HttpClientService[_, _]]

    before {
        keystoneV3Handler = new KeystoneV3Handler(new KeystoneV3Config(), mockAkkaServiceClient, mockDatastoreService, mockConnectionPoolService)
    }

    describe("handleRequest")(pending)

    describe("isUriWhitelisted") {
        it("should return true if uri is in the whitelist") {
            val whiteList = List("/test1", "/test2")
            val isUriWhitelisted = PrivateMethod[Boolean]('isUriWhitelisted)
            keystoneV3Handler invokePrivate isUriWhitelisted("/test1", whiteList) should be(true)
        }

        it("should return false if uri isn't in the whitelist") {
            val whiteList = List("/test1", "/test2")
            val isUriWhitelisted = PrivateMethod[Boolean]('isUriWhitelisted)
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
        it("should throw an exception when unable to retrieve admin token")(pending)
    }

    describe("createAdminAuthRequest") {
        it("should build a JSON auth token request without a domain ID") {
//            val createAdminAuthRequest = PrivateMethod[String]('createAdminAuthRequest)
//            keystoneV3Handler invokePrivate createAdminAuthRequest("user", "password") should equal ("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}")
            keystoneV3Handler.createAdminAuthRequest("user", "password") should equal ("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}")
        }

        it("should build a JSON auth token request with a none domain ID") {
            keystoneV3Handler.createAdminAuthRequest("user", "password", None) should equal ("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}")
        }

        it("should build a JSON auth token request with a string domain ID") {
            keystoneV3Handler.createAdminAuthRequest("user", "password", Option("domainId")) should equal ("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"domain\":{\"id\":\"domainId\"},\"name\":\"user\",\"password\":\"password\"}}}}}")
        }
    }
}
