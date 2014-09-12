package com.rackspace.papi.components.openstack.identity.basicauth

import com.rackspace.papi.components.openstack.identity.basicauth.config.OpenStackIdentityBasicAuthConfig
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityBasicAuthHandlerFactoryTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockDatastoreService = mock[DatastoreService]
  var handlerFactory: OpenStackIdentityBasicAuthHandlerFactory = _

  before {
    handlerFactory = new OpenStackIdentityBasicAuthHandlerFactory(mockAkkaServiceClient, mockDatastoreService)
  }

  describe("buildHandler") {
    it("should return a OpenStack Identity Basic Auth handler") {
      handlerFactory.configurationUpdated(new OpenStackIdentityBasicAuthConfig())
      handlerFactory.buildHandler shouldBe a[OpenStackIdentityBasicAuthHandler]
    }
  }

  describe("getListeners") {
    it("should return a map of listeners one of which listens to the OpenStack Identity configuration file") {
      val listeners = handlerFactory.getListeners

      listeners should have size 1
      listeners should contain key classOf[OpenStackIdentityBasicAuthConfig]
    }
  }
}
