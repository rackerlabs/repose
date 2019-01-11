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
package org.openrepose.nodeservice.containerconfiguration

import java.net.URL

import org.junit.runner.RunWith
import org.mockito.Mockito.{never, verify, when}
import org.mockito.{Matchers => MockitoMatchers}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config._
import org.openrepose.core.services.config.ConfigurationService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ContainerConfigurationServiceImplTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  final val DefaultClusterId = "defaultClusterId"

  var mockConfigService: ConfigurationService = _
  var containerConfigurationService: ContainerConfigurationServiceImpl = _

  override def beforeEach() = {
    mockConfigService = mock[ConfigurationService]
    containerConfigurationService = new ContainerConfigurationServiceImpl(DefaultClusterId, mockConfigService)
  }

  describe("init") {
    it("should register configuration listener") {
      containerConfigurationService.init()

      verify(mockConfigService).subscribeTo(
        MockitoMatchers.eq(ContainerConfigurationServiceImpl.ContainerConfigurationFilename),
        MockitoMatchers.any[URL](),
        MockitoMatchers.any[UpdateListener[ContainerConfiguration]](),
        MockitoMatchers.any[Class[ContainerConfiguration]]()
      )
    }
  }

  describe("destroy") {
    it("should unregister configuration listener") {
      containerConfigurationService.destroy()

      verify(mockConfigService).unsubscribeFrom(
        MockitoMatchers.eq(ContainerConfigurationServiceImpl.ContainerConfigurationFilename),
        MockitoMatchers.any[UpdateListener[ContainerConfiguration]]()
      )
    }
  }

  describe("isInitialized") {
    it("should return false if a configuration has not yet been read") {
      containerConfigurationService.isInitialized shouldBe false
    }

    it("should return true if a configuration has been read") {
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      containerConfigurationService.isInitialized shouldBe true
    }
  }

  describe("getRequestVia") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.getRequestVia
      }
    }

    it("should return an empty Optional if not configured") {
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      containerConfigurationService.getRequestVia.isPresent shouldBe false
    }

    it("should return the configured via string") {
      val config = minimalContainerConfiguration()
      val viaHeader = new ViaHeader
      viaHeader.setRequestPrefix("via")
      config.getDeploymentConfig.setViaHeader(viaHeader)

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getRequestVia.get shouldEqual "via"
    }
  }

  describe("getResponseVia") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.getResponseVia
      }
    }

    it("should return an empty Optional if not configured") {
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      containerConfigurationService.getResponseVia.isPresent shouldBe false
    }

    it("should return the configured via string") {
      val config = minimalContainerConfiguration()
      val viaHeader = new ViaHeader
      viaHeader.setResponsePrefix("via")
      config.getDeploymentConfig.setViaHeader(viaHeader)

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getResponseVia.get shouldEqual "via"
    }
  }

  describe("includeViaReposeVersion") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.includeViaReposeVersion
      }
    }

    it("should return true if not present") {
      val config = minimalContainerConfiguration()
      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.includeViaReposeVersion shouldBe true
    }

    it("should return true if not configured") {
      val config = minimalContainerConfiguration()
      val viaHeader = new ViaHeader
      config.getDeploymentConfig.setViaHeader(viaHeader)
      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.includeViaReposeVersion shouldBe true
    }

    Seq(true, false).foreach { state =>
      it(s"should return the configured state '$state'") {
        val config = minimalContainerConfiguration()
        val viaHeader = new ViaHeader
        viaHeader.setReposeVersion(state)
        config.getDeploymentConfig.setViaHeader(viaHeader)

        containerConfigurationService.configurationUpdated(config)

        containerConfigurationService.includeViaReposeVersion shouldBe state
      }
    }
  }

  describe("getContentBodyReadLimit") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.getContentBodyReadLimit
      }
    }

    it("should return an empty Optional if not configured") {
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      containerConfigurationService.getContentBodyReadLimit.isPresent shouldBe false
    }

    it("should return the configured content body read limit") {
      val config = minimalContainerConfiguration()
      config.getDeploymentConfig.setContentBodyReadLimit(1000L)

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getContentBodyReadLimit.get shouldEqual 1000L
    }
  }

  describe("getDeploymentConfiguration") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.getDeploymentConfiguration
      }
    }

    it("should return the deployment configuration") {
      val config = minimalContainerConfiguration()
      val viaHeader = new ViaHeader()
      viaHeader.setRequestPrefix("base-via")
      config.getDeploymentConfig.setViaHeader(viaHeader)

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getDeploymentConfiguration should be theSameInstanceAs config.getDeploymentConfig
      containerConfigurationService.getDeploymentConfiguration.getViaHeader.getRequestPrefix shouldEqual "base-via"
    }
  }

  describe("subscribeTo") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.subscribeTo(mock[UpdateListener[DeploymentConfiguration]])
      }
      intercept[IllegalStateException] {
        containerConfigurationService.subscribeTo(mock[UpdateListener[DeploymentConfiguration]], false)
      }
    }

    it("should send an initial notification") {
      val mockListener = mock[UpdateListener[DeploymentConfiguration]]

      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.subscribeTo(mockListener, sendNotificationNow = true)

      verify(mockListener).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
    }

    it("should not send an initial notification") {
      val mockListener = mock[UpdateListener[DeploymentConfiguration]]

      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.subscribeTo(mockListener, sendNotificationNow = false)

      verify(mockListener, never).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
    }

    it("should default to sending an initial notification") {
      val mockListener = mock[UpdateListener[DeploymentConfiguration]]

      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.subscribeTo(mockListener)

      verify(mockListener).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
    }

    it("should send a notification when the container configuration is updated") {
      val mockListener = mock[UpdateListener[DeploymentConfiguration]]

      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.subscribeTo(mockListener, sendNotificationNow = false)
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      verify(mockListener).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
    }

    it("should send a notification to multiple subscribers, even if one fails on update") {
      val mockListener = mock[UpdateListener[DeploymentConfiguration]]
      val mockListenerTwo = mock[UpdateListener[DeploymentConfiguration]]
      when(mockListener.configurationUpdated(MockitoMatchers.any[DeploymentConfiguration]))
        .thenThrow(new RuntimeException())
      when(mockListenerTwo.configurationUpdated(MockitoMatchers.any[DeploymentConfiguration]))
        .thenThrow(new RuntimeException())

      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.subscribeTo(mockListener, sendNotificationNow = false)
      containerConfigurationService.subscribeTo(mockListenerTwo, sendNotificationNow = false)
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      verify(mockListener).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
      verify(mockListenerTwo).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
    }
  }

  describe("unsubscribeFrom") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.unsubscribeFrom(mock[UpdateListener[DeploymentConfiguration]])
      }
    }

    it("should accept an invalid listener and do nothing") {
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.unsubscribeFrom(mock[UpdateListener[DeploymentConfiguration]])
    }

    it("should not send a notification when the container configuration is updated") {
      val mockListener = mock[UpdateListener[DeploymentConfiguration]]

      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())
      containerConfigurationService.subscribeTo(mockListener, sendNotificationNow = false)
      containerConfigurationService.unsubscribeFrom(mockListener)
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      verify(mockListener, never).configurationUpdated(MockitoMatchers.any[DeploymentConfiguration])
    }
  }

  def minimalContainerConfiguration(): ContainerConfiguration = {
    val containerConfiguration = new ContainerConfiguration()
    val deploymentConfiguration = new DeploymentConfiguration()

    deploymentConfiguration.setDeploymentDirectory {
      val deploymentDirectory = new DeploymentDirectory()
      deploymentDirectory.setValue("/repose/deployments")
      deploymentDirectory
    }
    deploymentConfiguration.setArtifactDirectory {
      val artifactDirectory = new ArtifactDirectory()
      artifactDirectory.setValue("/repose/artifacts")
      artifactDirectory
    }
    containerConfiguration.setDeploymentConfig(deploymentConfiguration)

    containerConfiguration
  }
}
