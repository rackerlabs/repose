package org.openrepose.filters.rackspaceidentitybasicauth

import org.junit.runner.RunWith
import org.openrepose.services.datastore.DatastoreService
import org.openrepose.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.rackspaceidentitybasicauth.config.RackspaceIdentityBasicAuthConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RackspaceIdentityBasicAuthHandlerFactoryTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockDatastoreService = mock[DatastoreService]
  var handlerFactory: RackspaceIdentityBasicAuthHandlerFactory = _

  before {
    handlerFactory = new RackspaceIdentityBasicAuthHandlerFactory(mockAkkaServiceClient, mockDatastoreService)
  }

  describe("buildHandler") {
    it("should return a Rackspace Identity Basic Auth handler") {
      handlerFactory.configurationUpdated(new RackspaceIdentityBasicAuthConfig())
      handlerFactory.buildHandler shouldBe a[RackspaceIdentityBasicAuthHandler]
    }
  }

  describe("getListeners") {
    it("should return a map of listeners one of which listens to the Rackspace Identity configuration file") {
      val listeners = handlerFactory.getListeners

      listeners should have size 1
      listeners should contain key classOf[RackspaceIdentityBasicAuthConfig]
    }
  }
}
