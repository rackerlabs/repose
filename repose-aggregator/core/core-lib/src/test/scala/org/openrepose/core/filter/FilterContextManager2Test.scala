package org.openrepose.core.filter

import javax.servlet.FilterConfig

import com.oracle.javaee6.{FullyQualifiedClassType, FilterType}
import org.openrepose.commons.utils.classloader.ear.{EarClassLoader, SimpleEarClassLoaderContext, EarDescriptor, EarClassLoaderContext}
import org.openrepose.core.spring.TestFilterBundlerHelper
import org.openrepose.core.systemmodel.Filter
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class FilterContextManager2Test extends FunSpec with Matchers with MockitoSugar with TestFilterBundlerHelper{

  def mockEarClassLoader(): EarClassLoaderContext = {
    import org.mockito.Mockito.when

    val earContext = mock[SimpleEarClassLoaderContext]
    val earDescriptor = new EarDescriptor()

    val filterType = new FilterType()
    val whatTheFuck = new FullyQualifiedClassType
    whatTheFuck.setValue("org.openrepose.filters.core.test.TestFilter")
    filterType.setFilterClass(whatTheFuck)

    earDescriptor.getRegisteredFiltersMap.put("test-filter", filterType)

    when(earContext.getEarDescriptor()).thenReturn(earDescriptor)

    val earClassLoader = new EarClassLoader(testFilterBundleClassLoader, testFilterBundleRoot)
    when(earContext.getClassLoader).thenReturn(earClassLoader)

    earContext
  }

  it("loads a filter context"){
    val classLoaderContext = mockEarClassLoader()

    import scala.collection.JavaConverters._

    val list =List(classLoaderContext).asJava
    val mockFilterConfig = mock[FilterConfig]

    val fcm = new FilterContextManager(mockFilterConfig)

    val jaxbFilterConfig = new Filter()
    jaxbFilterConfig.setName("test-filter")

    val filterContext = fcm.loadFilterContext(jaxbFilterConfig, list)

    filterContext shouldNot be(null)
  }
  describe("Throws a FilterInitializationException when"){
    it("the filter is not annotated with @Inject"){
      pending
    }
    it("the filter is not of the required type (javax.servlet.Filter)"){
      pending
    }
    it("the requested filter does not even exist") {
      pending
    }
    it("the requested filter fails to initialize") {
      pending
    }
    it("when there is no artifact to satisfy the filter request") {
      pending
    }
  }
}
