package org.openrepose.core.spring

import javax.servlet.Filter

import org.junit.runner.RunWith
import org.openrepose.core.spring.test.foo.FooBean
import org.openrepose.core.spring.test.{HerpBean, DerpBean}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Ignore, Matchers, FunSpec}
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext

@RunWith(classOf[JUnitRunner])
class CoreSpringProviderTest extends FunSpec with Matchers with TestFilterBundlerHelper {

  val coreSpringProvider = CoreSpringProvider.getInstance()

  describe("The Core Spring Provider") {
    it("is a singleton as the primary interface") {
      coreSpringProvider shouldBe CoreSpringProvider.getInstance()
      //Scala won't even compile with a private constructor access, so don't test it
    }
    it("provides the core service context"){
      val coreContext = coreSpringProvider.getCoreContext
      classOf[ApplicationContext].isAssignableFrom(coreContext.getClass) shouldBe true
    }
  }
  describe("Core Spring Container") {
    it("provides all beans from a specific package onward") {
      val coreContext = coreSpringProvider.getCoreContext

      val derpBean = coreContext.getBean[DerpBean](classOf[DerpBean])
      val herpBean = coreContext.getBean[HerpBean](classOf[HerpBean])
      val fooBean = coreContext.getBean[FooBean](classOf[FooBean])

      derpBean shouldNot be(null)
      herpBean shouldNot be(null)
      fooBean shouldNot be(null)

    }
    it("has a meaningful name for the core Context") {
      coreSpringProvider.getCoreContext.getDisplayName shouldBe "ReposeCoreContext"
    }
    it("provides a per-filter context from a given classloader") {
      val classLoader = testFilterBundleClassLoader

      val filterBeanContext = CoreSpringProvider.getContextForFilter(coreSpringProvider.getCoreContext, classLoader, "org.openrepose.filters.core.test.TestFilter", "TestFilterContextName")
      intercept[ClassNotFoundException] {
        CoreSpringProvider.getContextForFilter(coreSpringProvider.getCoreContext, getClass.getClassLoader, "org.openrepose.filters.core.test.TestFilter", "TestFilterContextBaseLoader")
      }
      filterBeanContext.getDisplayName should be("TestFilterContextName")

      val actualFilter = filterBeanContext.getBean[Filter](classOf[Filter])

      classOf[Filter].isAssignableFrom(actualFilter.getClass) shouldBe true
      actualFilter.getClass.getSimpleName shouldBe "TestFilter"
    }
    it("provides a closeable filter context") {
      val classLoader = this.getClass.getClassLoader
      val filterBeanContext = CoreSpringProvider.getContextForFilter(coreSpringProvider.getCoreContext,
        classLoader,
        "org.openrepose.core.spring.testfilter.TestFilter",
        "TestFilterContextName")

      //This should compile and close the context!
      filterBeanContext.close()

      intercept[NoSuchBeanDefinitionException] {
        filterBeanContext.getBean("org.openrepose.core.spring.testfilter.TestFilter")
      }
    }
  }

}
