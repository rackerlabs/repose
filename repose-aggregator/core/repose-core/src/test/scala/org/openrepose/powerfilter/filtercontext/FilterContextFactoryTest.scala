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
package org.openrepose.powerfilter.filtercontext

import javax.servlet.ServletContext

import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.classloader.{EarClassLoaderContext, EarClassProvider, ReallySimpleEarClassLoaderContext}
import org.openrepose.core.services.classloader.ClassLoaderManagerService
import org.openrepose.core.spring.{CoreSpringProvider, TestFilterBundlerHelper}
import org.openrepose.core.systemmodel.Filter
import org.openrepose.powerfilter.FilterInitializationException
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class FilterContextFactoryTest extends FunSpec with Matchers with MockitoSugar with TestFilterBundlerHelper {

  import org.mockito.Mockito.when

  import scala.collection.JavaConverters._

  CoreSpringProvider.getInstance().initializeCoreContext("/config/root", false);

  val appContext = CoreSpringProvider.getInstance().getCoreContext

  val mockServletContext = mock[ServletContext]

  def mockClassloaderManagerService(classMapping: Map[String, String]): ClassLoaderManagerService = {
    import org.mockito.Matchers.anyString

    val clms = mock[ClassLoaderManagerService]
    val mockList = List(reposeEarClassLoader(classMapping)).asJava
    when(clms.getLoadedApplications()).thenReturn(mockList)
    when(clms.hasFilter(anyString())).thenAnswer(new Answer[Boolean]() {
      override def answer(invocation: InvocationOnMock): Boolean = {
        val args = invocation.getArguments
        val arg: String = args(0).asInstanceOf[String]

        classMapping.keySet.contains(arg)
      }
    })

    clms
  }

  def reposeEarClassLoader(classMapping: Map[String, String]): EarClassLoaderContext = {
    //Use the new ear provider to get the classloader for the tests
    val earProvider = new EarClassProvider(testFilterBundleFile, testFilterBundleRoot)
    new ReallySimpleEarClassLoaderContext(earProvider.getEarDescriptor(), earProvider.getClassLoader())
  }

  it("loads a filter context") {
    val clms = mockClassloaderManagerService(Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter"))
    val list = List(clms).asJava

    val fcm = new FilterContextFactory(appContext, clms)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")

    val filterContexts = fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)

    filterContexts shouldNot be(null)
    filterContexts shouldNot be(empty)

    filterContexts.size() shouldBe 1

    val thingy = clms.getLoadedApplications.asScala.head.getClassLoader
    val clazz = thingy.loadClass("org.openrepose.filters.core.test.TestFilter")
    filterContexts.get(0).getFilter.getClass.isAssignableFrom(clazz) shouldBe true
  }

  it("loads a filter context for a filter that isn't annotated as a spring bean") {
    val clms = mockClassloaderManagerService(
      Map("unannotated-filter" -> "org.openrepose.filters.core.unannotated.UnannotatedFilter"))

    val fcm = new FilterContextFactory(appContext, clms)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("unannotated-filter")

    val filterContexts = fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)

    filterContexts shouldNot be(null)
    filterContexts shouldNot be(empty)

    filterContexts.size() shouldBe 1

    val thingy = clms.getLoadedApplications.asScala.head.getClassLoader
    val clazz = thingy.loadClass("org.openrepose.filters.core.unannotated.UnannotatedFilter")
    filterContexts.get(0).getFilter.getClass.isAssignableFrom(clazz) shouldBe true
  }

  it("loads a filter context when there's many filters") {
    val clms = mockClassloaderManagerService(Map(
      "test-filter" -> "org.openrepose.filters.core.test.TestFilter",
      "unannotated-filter" -> "org.openrepose.filters.core.unannotated.UnannotatedFilter"
    ))

    val fcm = new FilterContextFactory(appContext, clms)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")

    val filterContexts = fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)

    filterContexts shouldNot be(null)
    filterContexts shouldNot be(empty)

    val thingy = clms.getLoadedApplications.asScala.head.getClassLoader
    val clazz = thingy.loadClass("org.openrepose.filters.core.test.TestFilter")

    filterContexts.get(0).getFilter.getClass.isAssignableFrom(clazz) shouldBe true
  }

  it("will load multiple filter contexts for the same filter name") {
    val clms = mockClassloaderManagerService(
      Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter")
    )

    val fcm = new FilterContextFactory(appContext, clms)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")
    jaxbFilterConfig.setId("filter1")
    val jaxbFilterConfig2 = new Filter()
    jaxbFilterConfig2.setName("test-filter")
    jaxbFilterConfig2.setId("filter2")

    val filterContexts = fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig, jaxbFilterConfig2).asJava)
    filterContexts shouldNot be(null)
    filterContexts shouldNot be(empty)
    filterContexts.size() shouldBe 2

    filterContexts.get(0).getFilter shouldNot be(filterContexts.get(1).getFilter)
  }

  describe("filter context naming") {
    it("with the filter name and a uuid") {
      val clms = mockClassloaderManagerService(
        Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter")
      )

      val fcm = new FilterContextFactory(appContext, clms)

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName("test-filter")

      val filterContexts = fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)

      filterContexts.size shouldBe 1
      filterContexts.get(0).getFilterAppContext.getDisplayName should startWith("test-filter-")
    }
    it("with the id, name, and uuid") {
      val clms = mockClassloaderManagerService(
        Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter")
      )

      val fcm = new FilterContextFactory(appContext, clms)

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName("test-filter")
      jaxbFilterConfig.setId("my-test-id")

      val filterContexts = fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)

      filterContexts.size shouldBe 1
      filterContexts.get(0).getFilterAppContext.getDisplayName should startWith("my-test-id-test-filter-")
    }
  }

  describe("Throws a FilterInitializationException when") {

    val classMap = Map(
      "test-filter" -> "org.openrepose.filters.core.test.TestFilter",
      "broken-filter" -> "org.openrepose.filters.core.brokenfilter.BrokenFilter",
      "annotated-not-filter" -> "org.openrepose.filters.core.annotatednotfilter.AnnotatedNotFilter",
      "nonexistent-filter" -> "org.openrepose.filters.core.test.NopesFilter"
    )
    val clms = mockClassloaderManagerService(classMap)

    def failureTest(filterName: String)(f: (String, FilterInitializationException) => Unit) = {
      val fcm = new FilterContextFactory(appContext, clms)

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName(filterName)

      val exception = intercept[FilterInitializationException] {
        fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)
      }
      val className = classMap(filterName)
      f(className, exception)
    }

    it("the filter is not of the required type (javax.servlet.Filter)") {
      failureTest("annotated-not-filter") { (className, exception) =>
        exception.getMessage shouldBe s"Requested filter, ${className} is not of type javax.servlet.Filter"
      }
    }

    it("the requested filter does not even exist") {
      failureTest("nonexistent-filter") { (className, exception) =>
        exception.getMessage shouldBe s"Requested filter, ${className} does not exist in any loaded artifacts"
      }
    }
    it("the requested filter fails to initialize") {
      failureTest("broken-filter") { (className, exception) =>
        exception.getMessage shouldBe "Failed to initialize filter org.openrepose.filters.core.brokenfilter.BrokenFilter"
      }
    }
    it("when there is no artifact to satisfy the filter request") {
      val clms = mockClassloaderManagerService(Map.empty[String, String])

      val fcm = new FilterContextFactory(appContext, clms)

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName("nopenopenope")

      val exception = intercept[FilterInitializationException] {
        fcm.buildFilterContexts(mockServletContext, List(jaxbFilterConfig).asJava)
      }
      exception.getMessage shouldBe "Unable to satisfy requested filter chain - none of the loaded artifacts supply a filter named nopenopenope"
    }
  }
}
