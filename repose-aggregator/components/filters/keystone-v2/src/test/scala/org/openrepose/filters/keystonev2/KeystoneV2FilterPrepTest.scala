package org.openrepose.filters.keystonev2

import java.net.URL

import com.mockrunner.mock.web.MockFilterConfig
import org.junit.runner.RunWith
import org.mockito.{ArgumentCaptor, Matchers => MockitoMatcher, Mockito}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.keystonev2.config.KeystoneV2Config
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterPrepTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore: Datastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  val mockSystemModel = mock[SystemModel]
  Mockito.when(mockSystemModel.isTracingHeader).thenReturn(true, Nil: _*)

  before {
    Mockito.reset(mockDatastore)
  }

  describe("when the filter is initialized") {
    it("should initialize the configuration") {
      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val mockConfigService = mock[ConfigurationService]
      val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

      val config: MockFilterConfig = new MockFilterConfig
      config.setFilterName("KeystoneV2Filter")

      filter.init(config)

      val resourceCaptor = ArgumentCaptor.forClass(classOf[URL])
      Mockito.verify(mockConfigService).subscribeTo(
        MockitoMatcher.eq("KeystoneV2Filter"),
        MockitoMatcher.eq("keystone-v2.cfg.xml"),
        resourceCaptor.capture,
        MockitoMatcher.eq(filter.KeystoneV2ConfigListener),
        MockitoMatcher.eq(classOf[KeystoneV2Config]))
      Mockito.verify(mockConfigService).subscribeTo(
        MockitoMatcher.eq("system-model.cfg.xml"),
        MockitoMatcher.any(classOf[URL]),
        MockitoMatcher.eq(filter.SystemModelConfigListener),
        MockitoMatcher.eq(classOf[SystemModel]))

      assert(resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/keystone-v2.xsd"))
    }

    it("should initialize a configuration with a different name") {
      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val mockConfigService = mock[ConfigurationService]
      val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

      val config: MockFilterConfig = new MockFilterConfig
      config.setFilterName("KeystoneV2Filter")
      config.setInitParameter("filter-config", "some-other-config.xml")

      filter.init(config)

      val resourceCaptor = ArgumentCaptor.forClass(classOf[URL])
      Mockito.verify(mockConfigService).subscribeTo(
        MockitoMatcher.eq("KeystoneV2Filter"),
        MockitoMatcher.eq("some-other-config.xml"),
        resourceCaptor.capture,
        MockitoMatcher.eq(filter.KeystoneV2ConfigListener),
        MockitoMatcher.eq(classOf[KeystoneV2Config]))
      Mockito.verify(mockConfigService).subscribeTo(
        MockitoMatcher.eq("system-model.cfg.xml"),
        MockitoMatcher.any(classOf[URL]),
        MockitoMatcher.eq(filter.SystemModelConfigListener),
        MockitoMatcher.eq(classOf[SystemModel]))

      assert(resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/keystone-v2.xsd"))
    }
  }

  it("deregisters from the configuration service when destroying") {
    val mockConfigService = mock[ConfigurationService]
    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mock[AkkaServiceClient], mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.destroy()

    Mockito.verify(mockConfigService).unsubscribeFrom("keystone-v2.cfg.xml", filter.KeystoneV2ConfigListener)
  }

  describe("when the configuration is updated") {
    it("sets the current configuration on the filter asserting the defaults and initialized is true") {
      val filter = new KeystoneV2Filter(mock[ConfigurationService], mock[AkkaServiceClient], mockDatastoreService)
      filter.isInitialized shouldNot be(right = true)

      val configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |<identity-service
          |  username="user"
          |  password="pass"
          |  uri="https://lol.com"
          |  />
          |</keystone-v2>
        """.stripMargin)

      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val timeouts = filter.keystoneV2Config.getCache.getTimeouts
      timeouts.getEndpoints should be(600)
      timeouts.getGroup should be(600)
      timeouts.getToken should be(600)
      timeouts.getVariability should be(0)

      filter.keystoneV2Config.getIdentityService.isSetGroupsInHeader should be(right = true)
      filter.keystoneV2Config.getIdentityService.isSetCatalogInHeader should be(right = false)

      filter.keystoneV2Config.getDelegating should be(null)

      filter.keystoneV2Config.getWhiteList.getUriRegex.size() shouldBe 0

      filter.keystoneV2Config.getTenantHandling.getValidateTenant should be(null)

      filter.keystoneV2Config.getRequireServiceEndpoint should be(null)
    }

    it("sets the default delegating quality to 0.7") {
      val filter = new KeystoneV2Filter(mock[ConfigurationService], mock[AkkaServiceClient], mockDatastoreService)
      filter.isInitialized shouldNot be(right = true)

      val configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |<delegating/>
          |<identity-service
          |  username="user"
          |  password="pass"
          |  uri="https://lol.com"
          |  />
          |</keystone-v2>
        """.stripMargin)

      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val timeouts = filter.keystoneV2Config.getCache.getTimeouts
      filter.keystoneV2Config.getDelegating.getQuality should be(0.7)
    }
  }
}
