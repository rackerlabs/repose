package org.openrepose.core.spring

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, FunSpec}
import org.springframework.context.ApplicationContext

@RunWith(classOf[JUnitRunner])
class CoreSpringProviderTest extends FunSpec with Matchers {

  describe("The Core Spring Provider") {
    it("is a singleton as the primary interface") {
      val coreSpringProvider = CoreSpringProvider.getInstance()

      coreSpringProvider shouldBe CoreSpringProvider.getInstance()
      //Scala won't even compile with a private constructor access, so don't test it
    }
    it("provides the core service context"){
      val coreSpringProvider = CoreSpringProvider.getInstance()

      val coreContext = coreSpringProvider.getCoreContext
      classOf[ApplicationContext].isAssignableFrom(coreContext.getClass) shouldBe true
    }
  }

}
