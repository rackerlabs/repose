package org.openrepose.core.filter

import javax.servlet.FilterConfig

import com.oracle.javaee6.{FilterType, FullyQualifiedClassType}
import org.junit.runner.RunWith
import org.openrepose.commons.utils.classloader.ear.{EarClassLoader, EarClassLoaderContext, EarDescriptor, SimpleEarClassLoaderContext}
import org.openrepose.core.spring.TestFilterBundlerHelper
import org.openrepose.core.systemmodel.Filter
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class FilterContextManagerTest extends FunSpec with Matchers with MockitoSugar with TestFilterBundlerHelper {

  def mockEarClassLoader(classMapping: Map[String, String]): EarClassLoaderContext = {
    import org.mockito.Mockito.when

    val earContext = mock[SimpleEarClassLoaderContext]
    val earDescriptor = new EarDescriptor()


    classMapping.foreach { case (filterName, filterClass) =>
      val filterType = new FilterType()
      val fullyQualifiedClassType = new FullyQualifiedClassType
      fullyQualifiedClassType.setValue(filterClass)
      filterType.setFilterClass(fullyQualifiedClassType)

      earDescriptor.getRegisteredFiltersMap.put(filterName, filterType)
    }

    when(earContext.getEarDescriptor()).thenReturn(earDescriptor)

    val earClassLoader = new EarClassLoader(testFilterBundleClassLoader, testFilterBundleRoot)
    when(earContext.getClassLoader).thenReturn(earClassLoader)

    earContext
  }

  it("loads a filter context") {
    val classLoaderContext = mockEarClassLoader(Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter"))

    import scala.collection.JavaConverters._

    val list = List(classLoaderContext).asJava
    val mockFilterConfig = mock[FilterConfig]

    val fcm = new FilterContextManager(mockFilterConfig)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")

    val filterContext = fcm.loadFilterContext(jaxbFilterConfig, list)

    filterContext shouldNot be(null)

    val clazz = testFilterBundleClassLoader.loadClass("org.openrepose.filters.core.test.TestFilter")
    filterContext.getFilter.getClass.isAssignableFrom(clazz) shouldBe true

  }
  it("loads a filter context when there's many filters") {
    val classLoaderContext = mockEarClassLoader(Map(
      "test-filter" -> "org.openrepose.filters.core.test.TestFilter",
      "unannotated-filter" -> "org.openrepose.filters.core.unannotated.UnannotatedFilter"
    ))

    import scala.collection.JavaConverters._

    val list = List(classLoaderContext).asJava
    val mockFilterConfig = mock[FilterConfig]

    val fcm = new FilterContextManager(mockFilterConfig)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")

    val filterContext = fcm.loadFilterContext(jaxbFilterConfig, list)

    filterContext shouldNot be(null)

    val clazz = testFilterBundleClassLoader.loadClass("org.openrepose.filters.core.test.TestFilter")

    filterContext.getFilter.getClass.isAssignableFrom(clazz) shouldBe true
  }

  it("will load multiple filter contexts for the same filter name") {
    val classLoaderContext = mockEarClassLoader(Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter"))

    import scala.collection.JavaConverters._

    val list = List(classLoaderContext).asJava
    val mockFilterConfig = mock[FilterConfig]

    val fcm = new FilterContextManager(mockFilterConfig)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")

    val filterContext = fcm.loadFilterContext(jaxbFilterConfig, list)
    filterContext shouldNot be(null)

    val filterContext2 = fcm.loadFilterContext(jaxbFilterConfig, list)

    filterContext2 shouldNot be(null)

    filterContext.getFilter shouldNot be(filterContext2.getFilter)

  }
  describe("filter context naming") {
    it("with the filter name and a uuid") {
      val classLoaderContext = mockEarClassLoader(Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter"))

      import scala.collection.JavaConverters._

      val list = List(classLoaderContext).asJava
      val mockFilterConfig = mock[FilterConfig]

      val fcm = new FilterContextManager(mockFilterConfig)

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName("test-filter")

      val filterContext = fcm.loadFilterContext(jaxbFilterConfig, list)
      filterContext shouldNot be(null)

      filterContext.getFilterAppContext.getDisplayName should startWith("test-filter-")

    }
    it("with the id, name, and uuid") {
      val classLoaderContext = mockEarClassLoader(Map("test-filter" -> "org.openrepose.filters.core.test.TestFilter"))

      import scala.collection.JavaConverters._

      val list = List(classLoaderContext).asJava
      val mockFilterConfig = mock[FilterConfig]

      val fcm = new FilterContextManager(mockFilterConfig)

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName("test-filter")
      jaxbFilterConfig.setId("my-test-id")

      val filterContext = fcm.loadFilterContext(jaxbFilterConfig, list)
      filterContext shouldNot be(null)

      filterContext.getFilterAppContext.getDisplayName should startWith("my-test-id-test-filter-")

    }
  }

  describe("Throws a FilterInitializationException when") {
    val classMap = Map(
      "test-filter" -> "org.openrepose.filters.core.test.TestFilter",
      "unannotated-filter" -> "org.openrepose.filters.core.unannotated.UnannotatedFilter",
      "broken-filter" -> "org.openrepose.filters.core.brokenfilter.BrokenFilter",
      "annotated-not-filter" -> "org.openrepose.filters.core.annotatednotfilter.AnnotatedNotFilter",
      "nonexistent-filter" -> "org.openrepose.filters.core.nopes.NopesFilter"
    )
    val classLoaderContext = mockEarClassLoader(classMap)

    def failureTest(filterName: String)(f: (String, FilterInitializationException) => Unit) = {
      import scala.collection.JavaConverters._
      val list = List(classLoaderContext).asJava

      val fcm = new FilterContextManager(mock[FilterConfig])

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName(filterName)

      val exception = intercept[FilterInitializationException] {
        fcm.loadFilterContext(jaxbFilterConfig, list)
      }

      val className = classMap(filterName)
      f(className, exception)
    }

    it("the filter is not annotated with @Inject") {
      failureTest("unannotated-filter") { (className, exception) =>
        exception.getMessage should be(s"Requested filter, ${className} is not an annotated Component. Make sure your filter is an annotated Spring Bean.")
      }
    }
    it("the filter is not of the required type (javax.servlet.Filter)") {
      failureTest("annotated-not-filter") { (className, exception) =>
        exception.getMessage should be(s"Requested filter, ${className} is not of type javax.servlet.Filter")
      }
    }
    it("the requested filter does not even exist") {
      failureTest("nonexistent-filter") { (className, exception) =>
        exception.getMessage should be(s"Requested filter, ${className} does not exist in any loaded artifacts")
      }
    }
    it("the requested filter fails to initialize") {
      failureTest("broken-filter") { (className, exception) =>
        exception.getMessage should be("WELL MY HEAD ASPLODE")
      }
    }
    it("when there is no artifact to satisfy the filter request") {
      import scala.collection.JavaConverters._
      val list = List(classLoaderContext).asJava

      val fcm = new FilterContextManager(mock[FilterConfig])

      val jaxbFilterConfig = new Filter()
      jaxbFilterConfig.setName("nopenopenope")

      val exception = intercept[FilterInitializationException] {
        fcm.loadFilterContext(jaxbFilterConfig, list)
      }
      exception.getMessage should be("No deployed artifact found to satisfy filter named: nopenopenope")
    }
  }
}
