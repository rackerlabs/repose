package org.openrepose.core.spring

import javax.servlet.Filter

import org.openrepose.core.spring.test.foo.FooBean
import org.openrepose.core.spring.test.{DerpBean, HerpBean}
import org.scalatest.{FunSpec, Matchers}

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
    it("provides a per-filter context from a given classloader"){
      // TODO: How to verify that we're doing this actual classloader
      // TODO: do we build a test support ear for core to verify its interaction
      val csc = new CoreSpringContainer("org.openrepose.core.spring.test")
      val classLoader = this.getClass.getClassLoader
      val filterBeanContext = csc.getContextForFilter(classLoader, "org.openrepose.core.spring.testfilter.TestFilter", "TestFilterContextName")

      filterBeanContext.getDisplayName should be("TestFilterContextName")

      val actualFilter = filterBeanContext.getBean[Filter](classOf[Filter])

      classOf[Filter].isAssignableFrom(actualFilter.getClass) shouldBe true
    }
    it("closes down a filter context") {
      pending
    }
  }
}
