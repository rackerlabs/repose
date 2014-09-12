package com.rackspace.papi.components.keystone.basicauth

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.datastore.Datastore
import com.rackspace.papi.components.keystone.basicauth.config.KeystoneBasicAuthConfig
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
class KeystoneBasicAuthHandlerTest extends FunSpec with BeforeAndAfter with Matchers with PrivateMethodTester with MockitoSugar {

  var keystoneBasicAuthHandler: KeystoneBasicAuthHandler = _
  var keystoneBasicAuthConfig: KeystoneBasicAuthConfig = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockDatastoreService: DatastoreService = _
  var mockDatastore: Datastore = _

  before {
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockDatastoreService = mock[DatastoreService]
    mockDatastore = mock[Datastore]
    keystoneBasicAuthConfig = new KeystoneBasicAuthConfig()
    keystoneBasicAuthConfig.setTodoAttribute(true)
    keystoneBasicAuthConfig.setTodoElement(true)

    when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)

    keystoneBasicAuthHandler = new KeystoneBasicAuthHandler(keystoneBasicAuthConfig, mockAkkaServiceClient, mockDatastoreService)
  }

  describe("handleRequest") {
    it("should simply pass if there is not an HTTP Basic authentication header") {
      // given: "a mock'd ServletRequest and ServletResponse"
      val mockServletRequest = mock[HttpServletRequest]
      val mockServletResponse = mock[ReadableHttpServletResponse]

      // when: "the filter's/handler's handleRequest() is called without an HTTP Basic authentication header"
      val filterDirector = keystoneBasicAuthHandler.handleRequest(mockServletRequest, mockServletResponse)

      // then: "the filter's response status code should be 200."
      filterDirector.getFilterAction should be theSameInstanceAs FilterAction.PASS
      filterDirector.getResponseStatusCode should be (HttpServletResponse.SC_OK) // 200
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
  //    //when(mockServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK) // 200
  //
  //    // when: "the filter's/handler's handleResponse() is called"
  //    val filterDirector = keystoneBasicAuthHandler.handleResponse(mockServletRequest, mockServletResponse)
  //
  //    // then: "the filter's response status code should be No Content"
  //    filterDirector.getResponseStatusCode should be(HttpServletResponse.SC_NO_CONTENT) // 204
  //  }
  //}
}
