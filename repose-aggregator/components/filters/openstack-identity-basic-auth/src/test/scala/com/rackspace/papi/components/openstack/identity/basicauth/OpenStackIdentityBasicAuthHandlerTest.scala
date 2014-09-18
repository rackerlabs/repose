package com.rackspace.papi.components.openstack.identity.basicauth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.datastore.Datastore
import com.rackspace.papi.components.openstack.identity.basicauth.config.OpenStackIdentityBasicAuthConfig
import com.rackspace.papi.filter.logic.FilterAction
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityBasicAuthHandlerTest extends FunSpec with BeforeAndAfter with Matchers with PrivateMethodTester with MockitoSugar {

  var openStackIdentityBasicAuthHandler: OpenStackIdentityBasicAuthHandler = _
  var openStackIdentityBasicAuthConfig: OpenStackIdentityBasicAuthConfig = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockDatastoreService: DatastoreService = _
  var mockDatastore: Datastore = _

  before {
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockDatastoreService = mock[DatastoreService]
    mockDatastore = mock[Datastore]
    openStackIdentityBasicAuthConfig = new OpenStackIdentityBasicAuthConfig()
    openStackIdentityBasicAuthConfig.setTokenCacheTimeoutMillis(0)

    when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)

    openStackIdentityBasicAuthHandler = new OpenStackIdentityBasicAuthHandler(openStackIdentityBasicAuthConfig, mockAkkaServiceClient, mockDatastoreService)
  }

  describe("handleRequest") {
    it("should simply pass if there is not an HTTP Basic authentication header") {
      // given: "a mock'd ServletRequest and ServletResponse"
      val mockServletRequest = mock[HttpServletRequest]
      val mockServletResponse = mock[ReadableHttpServletResponse]

      // when: "the filter's/handler's handleRequest() is called without an HTTP Basic authentication header"
      val filterDirector = openStackIdentityBasicAuthHandler.handleRequest(mockServletRequest, mockServletResponse)

      // then: "the filter's response status code should be UNAUTHORIZED (401)."
      filterDirector.getFilterAction should be theSameInstanceAs FilterAction.PROCESS_RESPONSE
      filterDirector.getResponseStatusCode should be(HttpServletResponse.SC_UNAUTHORIZED)
    }
  }

  // Due to the apparent limitation of the current mock environment,
  // this test will be moved to a Spock functional test.
  //describe("handleResponse") {
  //  it("should pass filter") {
  //    // given: "a mock'd ServletRequest and ServletResponse"
  //    val mockServletRequest = mock[HttpServletRequest]
  //    val mockServletResponse = mock[ReadableHttpServletResponse]
  //
  //    // TODO: This should work, but seems to be a limitation of the current ScalaMock.
  //    //when(mockServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK)
  //
  //    // when: "the filter's/handler's handleResponse() is called"
  //    val filterDirector = openStackIdentityBasicAuthHandler.handleResponse(mockServletRequest, mockServletResponse)
  //
  //    // then: "the filter's response status code should be No Content (204)"
  //    filterDirector.getResponseStatusCode should be(HttpServletResponse.SC_NO_CONTENT)
  //  }
  //}
}
