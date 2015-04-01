package org.openrepose.filters.valkyrieauthorization

import java.net.URL

import com.mockrunner.mock.web.MockFilterConfig
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.junit.runner.RunWith
import org.mockito.{ArgumentCaptor, Mockito, Matchers}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.{ValkyrieServer, DelegatingType, ValkyrieAuthorizationConfig}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValkyrieAuthorizationFilterTest extends FunSpec with MockitoSugar {

  describe("when initializing the filter") {
    it("should initialize the configuration to a given configuration") {
      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mockAkkaServiceClient)

      val config: MockFilterConfig = new MockFilterConfig
      config.setFilterName("ValkyrieFilter")

      filter.init(config)

      val resourceCaptor = ArgumentCaptor.forClass(classOf[URL])
      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.eq("ValkyrieFilter"),
        Matchers.eq("valkyrie-authorization.cfg.xml"),
        resourceCaptor.capture,
        Matchers.eq(filter),
        Matchers.eq(classOf[ValkyrieAuthorizationConfig]))

      assert(resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/valkyrie-authorization.xsd"))
    }
    it("should initialize the configuration to a given name") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mock[AkkaServiceClient])

      val config: MockFilterConfig = new MockFilterConfig
      config.setInitParameter("filter-config", "another-name.cfg.xml")

      filter.init(config)

      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.anyString,
        Matchers.eq("another-name.cfg.xml"),
        Matchers.any(classOf[URL]),
        Matchers.any(classOf[ValkyrieAuthorizationFilter]),
        Matchers.eq(classOf[ValkyrieAuthorizationConfig]))
    }
  }

  describe("when destroying the filter") {
    it("should deregister the configuration from the configuration service") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mock[AkkaServiceClient])

      val config: MockFilterConfig = new MockFilterConfig
      filter.init(config)
      filter.destroy

      Mockito.verify(mockConfigService).unsubscribeFrom("valkyrie-authorization.cfg.xml", filter)
    }
  }
  
  describe("when the configuration is updated") {
    it("should set the current configuration on the filter with the defaults initially and flag that it is initialized") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], mock[AkkaServiceClient])

      assert(!filter.isInitialized)

      val configuration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(configuration)

      assert(filter.configuration == configuration)
      assert(filter.configuration.getDelegating == null)
      assert(filter.configuration.getCacheTimeoutMillis == 300000)
      assert(filter.isInitialized)
    }
    it("should set the default delegation quality to .1") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], mock[AkkaServiceClient])

      assert(filter.configuration == null)

      val configuration = new ValkyrieAuthorizationConfig
      val delegation = new DelegatingType
      configuration.setDelegating(delegation)
      filter.configurationUpdated(configuration)

      assert(filter.configuration.getDelegating.getQuality == .1)
    }
    it("should set the configuration to current") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mock[AkkaServiceClient])

      val configuration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(configuration)

      assert(filter.configuration == configuration)
      assert(filter.isInitialized)

      val newConfiguration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(newConfiguration)

      assert(filter.configuration == newConfiguration)
      assert(filter.isInitialized)
    }
  }
}
