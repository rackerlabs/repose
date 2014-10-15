package org.openrepose.core.spring

import org.openrepose.core.spring.test.foo.FooBean
import org.openrepose.core.spring.test.{HerpBean, DerpBean}
import org.scalatest.{FunSpec, Matchers}
import org.springframework.context.ApplicationContext

class CoreSpringContainerTest extends FunSpec with Matchers{

  describe("Core Spring Container") {
    it("provides all beans from a specific package onward"){
      val csc = new CoreSpringContainer("org.openrepose.core.spring.test")

      val coreContext = csc.getCoreContext

      val derpBean = coreContext.getBean[DerpBean](classOf[DerpBean])
      val herpBean = coreContext.getBean[HerpBean](classOf[HerpBean])
      val fooBean = coreContext.getBean[FooBean](classOf[FooBean])

      derpBean shouldNot be(null)
      herpBean shouldNot be(null)
      fooBean shouldNot be(null)

    }
    it("has a meaningful name for the core Context") {
      val csc = new CoreSpringContainer("org.nope.nothingtoscan")
      csc.getCoreContext.getDisplayName shouldBe "ReposeCoreContext"
    }
    it("provides a per-filter context"){
      pending
    }
    it("closes down a filter context") {
      pending
    }
  }
}
