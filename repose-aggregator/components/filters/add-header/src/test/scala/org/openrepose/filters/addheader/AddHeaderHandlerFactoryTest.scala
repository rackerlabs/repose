package org.openrepose.filters.addheader

import java.util

import org.junit.runner.RunWith
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.filters.addheader.config.{AddHeadersType, HeaderType}
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class AddHeaderHandlerFactoryTest extends FunSpec with BeforeAndAfter with PrivateMethodTester with Matchers with MockitoSugar {

  var handlerFactory: AddHeaderHandlerFactory = _

  before {
    handlerFactory = new AddHeaderHandlerFactory(List[HeaderType]())
  }

  describe("buildHandler") {
    ignore("should return an Add Header handler") {
      val config: AddHeadersType = new AddHeadersType()
      val header = new HeaderType()
      header.setName("x-new-header")
      header.getValue.add("new-value")
      header.setQuality(0.2)

      config.getHeader.add(header)
      val buildHandler = PrivateMethod[AddHeaderHandler]('buildHandler)

      handlerFactory.configurationUpdated(config)
      val handler = handlerFactory invokePrivate buildHandler()
      handler shouldBe an[AddHeaderHandler]
    }
  }

  describe("getListeners") {
    it("should return a map of listeners one of which listens to the Add Header configuration file") {
      val getListeners = PrivateMethod[util.Map[Class[_], UpdateListener[_]]]('getListeners)

      val listeners = handlerFactory invokePrivate getListeners()

      listeners should have size 1
      listeners should contain key classOf[AddHeadersType]
    }
  }
}
