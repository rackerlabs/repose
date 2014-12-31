package org.openrepose.filters.forwardedproto

import javax.servlet.http.{HttpServletResponse, HttpServletResponseWrapper}

import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

/**
 * Created by eric7500 on 12/31/14.
 */
class ForwardedProtoHandlerFactoryTest extends FunSpec with Matchers with PrivateMethodTester with BeforeAndAfter {
  var handlerFactory: ForwardedProtoHandlerFactory = _

  describe("buildHandler") {
    it("should return an ForwardedProtoHandler") {
      handlerFactory = new ForwardedProtoHandlerFactory()
      handlerFactory.buildHandler shouldBe a[ForwardedProtoHandler]
    }
  }
}


