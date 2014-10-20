package org.openrepose.core.spring

import javax.servlet.Filter

import org.openrepose.core.spring.test.foo.FooBean
import org.openrepose.core.spring.test.{DerpBean, HerpBean}
import org.scalatest.{FunSpec, Matchers}
import org.springframework.beans.factory.NoSuchBeanDefinitionException

class CoreSpringContainerTest extends FunSpec with Matchers with TestFilterBundlerHelper {

  describe("Core Spring Container") {
    it("provides all beans from a specific package onward") {
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
    it("provides a per-filter context from a given classloader") {
      val csc = new CoreSpringContainer("org.openrepose.core.spring.test")
      val classLoader = testFilterBundleClassLoader

      val filterBeanContext = csc.getContextForFilter(classLoader, "org.openrepose.filters.core.test.TestFilter", "TestFilterContextName")
      intercept[ClassNotFoundException] {
        csc.getContextForFilter(getClass.getClassLoader, "org.openrepose.filters.core.test.TestFilter", "TestFilterContextBaseLoader")
      }
      filterBeanContext.getDisplayName should be("TestFilterContextName")

      val actualFilter = filterBeanContext.getBean[Filter](classOf[Filter])

      classOf[Filter].isAssignableFrom(actualFilter.getClass) shouldBe true
      actualFilter.getClass.getSimpleName shouldBe "TestFilter"
    }
    it("provides a closeable filter context") {
      val csc = new CoreSpringContainer("org.openrepose.core.spring.test")

      val classLoader = this.getClass.getClassLoader
      val filterBeanContext = csc.getContextForFilter(classLoader, "org.openrepose.core.spring.testfilter.TestFilter", "TestFilterContextName")

      //This should compile and close the context!
      filterBeanContext.close()

      intercept[NoSuchBeanDefinitionException] {
        filterBeanContext.getBean("org.openrepose.core.spring.testfilter.TestFilter")
      }
    }
  }
}
