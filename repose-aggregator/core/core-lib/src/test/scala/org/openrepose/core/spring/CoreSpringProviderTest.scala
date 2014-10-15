package org.openrepose.core.spring

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, FunSpec}

@RunWith(classOf[JUnitRunner])
class CoreSpringProviderTest extends FunSpec with Matchers {

  describe("The Core Context Provider") {
    it("is a singleton as the primary interface") {
      val coreSpringProvider = CoreSpringProvider.getInstance()

      coreSpringProvider shouldBe CoreSpringProvider.getInstance()
      //Scala won't even compile with a private constructor access, so don't test it
    }
  }

}
