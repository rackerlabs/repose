package com.rackspace.papi.components.keystone.v3

import com.rackspace.papi.components.keystone.v3.config.{KeystoneV3Config, OpenstackKeystoneService}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneV3HandlerFactoryTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  var handlerFactory: KeystoneV3HandlerFactory = _

  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockDatastoreService = mock[DatastoreService]

  before {
    when(mockDatastoreService.getDefaultDatastore).thenReturn(null)

    handlerFactory = new KeystoneV3HandlerFactory(mockAkkaServiceClient, mockDatastoreService)
  }

  describe("buildHandler") {
    it("should return a Keystone v3 handler") {
      val keystoneService = new OpenstackKeystoneService()
      keystoneService.setUri("")

      val config = new KeystoneV3Config()
      config.setKeystoneService(keystoneService)
      config.setTokenCacheTimeout(0)
      config.setGroupsCacheTimeout(0)
      config.setCacheOffset(0)
      config.setForwardUnauthorizedRequests(false)

      handlerFactory.configurationUpdated(config)
      handlerFactory.buildHandler shouldBe a[KeystoneV3Handler]
    }
  }

  describe("getListeners") {
    it("should return a map of listeners one of which listens to the keystone configuration file") {
      val listeners = handlerFactory.getListeners

      listeners should have size 1
      listeners should contain key classOf[KeystoneV3Config]
    }
  }
}
