/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.spring

import javax.servlet.Filter
import org.junit.runner.RunWith
import org.openrepose.core.spring.test.foo.FooBean
import org.openrepose.core.spring.test.{DerpBean, HerpBean}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.springframework.context.ApplicationContext
import com.anycompany.spring.test.foo.TestFooBean
import com.anycompany.spring.test.TestBean
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor

@RunWith(classOf[JUnitRunner])
class CoreSpringProviderTest extends FunSpec with Matchers with TestFilterBundlerHelper {

  val coreSpringProvider = CoreSpringProvider.getInstance()
  coreSpringProvider.initializeCoreContext("/etc/repose", false)


  describe("The Core Spring Provider") {
    it("is a singleton as the primary interface") {
      coreSpringProvider shouldBe CoreSpringProvider.getInstance()
      //Scala won't even compile with a private constructor access, so don't test it
    }
    it("provides the core service context") {
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

      //Also assert that the custom package path beans got scanned
      val testBean = coreContext.getBean[TestBean](classOf[TestBean])
      val testFooBean = coreContext.getBean[TestFooBean](classOf[TestFooBean])

      testBean shouldNot be(null)
      testFooBean shouldNot be(null)
    }
    it("has a registered post processor for the scheduled annotation") {
      val coreContext = coreSpringProvider.getCoreContext

      val scheduledPostProcessors = coreContext.getBeansOfType(classOf[ScheduledAnnotationBeanPostProcessor])

      scheduledPostProcessors should have size 1
    }
    it("has a meaningful name for the core Context") {
      coreSpringProvider.getCoreContext.getDisplayName shouldBe "ReposeCoreContext"
    }

    //TODO: can't test this without an entire classloader manager service :(
    ignore("provides a per-filter context from a given classloader") {
      val classLoader = null

      val filterBeanContext = CoreSpringProvider.getContextForFilter(coreSpringProvider.getCoreContext, classLoader, "org.openrepose.filters.core.test.TestFilter", "TestFilterContextName")
      intercept[ClassNotFoundException] {
        CoreSpringProvider.getContextForFilter(coreSpringProvider.getCoreContext, getClass.getClassLoader, "org.openrepose.filters.core.test.TestFilter", "TestFilterContextBaseLoader")
      }
      filterBeanContext.getDisplayName shouldBe "TestFilterContextName"

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

      //In spring 4.1 when the filter context is closed, we get an IllegalStateException
      intercept[IllegalStateException] {
        filterBeanContext.getBean("org.openrepose.core.spring.testfilter.TestFilter")
      }
    }
  }
}
