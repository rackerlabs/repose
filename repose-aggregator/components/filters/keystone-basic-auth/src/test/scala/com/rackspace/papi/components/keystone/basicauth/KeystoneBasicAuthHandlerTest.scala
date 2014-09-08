package com.rackspace.papi.components.keystone.basicauth

import javax.servlet.http.HttpServletResponse

import com.mockrunner.mock.web.MockHttpServletRequest
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
    it("should pass filter") {
      val mockServletRequest = new MockHttpServletRequest()
      val mockServletResponse = mock[ReadableHttpServletResponse]
      keystoneBasicAuthHandler.handleRequest(mockServletRequest, mockServletResponse).getFilterAction should be theSameInstanceAs FilterAction.PASS
    }
  }

  describe("handleResponse") {
    it("should pass filter") {
      val mockServletRequest = new MockHttpServletRequest()
      val mockServletResponse = mock[ReadableHttpServletResponse]
      // TODO: This should work, but seems to be a limitation of the current ScalaMock.
      //when(mockServletResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK)
      keystoneBasicAuthHandler.handleResponse(mockServletRequest, mockServletResponse).getResponseStatusCode should be(HttpServletResponse.SC_NO_CONTENT)
    }
  }
}
