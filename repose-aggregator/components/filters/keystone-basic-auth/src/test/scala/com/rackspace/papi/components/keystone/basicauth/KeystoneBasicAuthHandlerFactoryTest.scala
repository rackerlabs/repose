package com.rackspace.papi.components.keystone.basicauth

import com.rackspace.papi.components.keystone.basicauth.config.KeystoneBasicAuthConfig
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneBasicAuthHandlerFactoryTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockDatastoreService = mock[DatastoreService]
  var handlerFactory: KeystoneBasicAuthHandlerFactory = _

  before {
    handlerFactory = new KeystoneBasicAuthHandlerFactory(mockAkkaServiceClient, mockDatastoreService)
  }

  describe("buildHandler") {
    it("should return a Keystone Basic Auth handler") {
      handlerFactory.configurationUpdated(new KeystoneBasicAuthConfig())
      handlerFactory.buildHandler shouldBe a[KeystoneBasicAuthHandler]
    }
  }

  describe("getListeners") {
    it("should return a map of listeners one of which listens to the keystone configuration file") {
      val listeners = handlerFactory.getListeners

      listeners should have size 1
      listeners should contain key classOf[KeystoneBasicAuthConfig]
    }
  }
}
