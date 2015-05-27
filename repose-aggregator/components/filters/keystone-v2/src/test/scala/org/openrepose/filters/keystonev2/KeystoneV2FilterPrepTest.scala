package org.openrepose.filters.keystonev2

import java.net.URL

import com.mockrunner.mock.web.MockFilterConfig
import org.junit.runner.RunWith
import org.mockito.{ArgumentCaptor, Matchers => MockitoMatcher, Mockito}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.Keystonev2.KeystoneV2Filter
import org.openrepose.filters.keystonev2.config.KeystoneV2Config
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterPrepTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {
  System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")


  val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore: Datastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)

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
        MockitoMatcher.eq(filter),
        MockitoMatcher.eq(classOf[KeystoneV2Config]))

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
        MockitoMatcher.eq(filter),
        MockitoMatcher.eq(classOf[KeystoneV2Config]))

      assert(resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/keystone-v2.xsd"))

    }
  }
  it("deregisters from the configuration service when destroying") {
    val mockConfigService = mock[ConfigurationService]
    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mock[AkkaServiceClient], mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.destroy

    Mockito.verify(mockConfigService).unsubscribeFrom("keystone-v2.cfg.xml", filter)

  }

  describe("when the configuration is updated") {
    it("sets the current configuration on the filter asserting the defaults and initialized is true") {
      val filter = new KeystoneV2Filter(mock[ConfigurationService], mock[AkkaServiceClient], mockDatastoreService)
      filter.isInitialized shouldNot be(true)

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

      filter.configurationUpdated(configuration)

      val timeouts = filter.configuration.getCacheSettings.getTimeouts
      timeouts.getEndpoints should be(60000)
      timeouts.getGroup should be(60000)
      timeouts.getToken should be(60000)
      timeouts.getUser should be(60000)
      timeouts.getVariability should be(0)

      filter.configuration.getDelegating should be(null)

      filter.configuration.getWhiteList should be(null)

      filter.configuration.getTenantHandling should be(null)

      filter.configuration.getForward should be(null)

      filter.configuration.getRequireServiceEndpoint should be(null)


    }
    it("sets the default delegating quality to 0.7") {
      pending
    }
  }
}
