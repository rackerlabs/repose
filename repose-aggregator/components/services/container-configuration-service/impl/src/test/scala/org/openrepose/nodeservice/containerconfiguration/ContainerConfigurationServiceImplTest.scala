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
import org.mockito.Mockito.verify
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

  describe("getVia") {
    it("should throw an Exception if the service is not yet initialized") {
      intercept[IllegalStateException] {
        containerConfigurationService.getVia
      }
    }

    it("should return an empty Optional if not configured") {
      containerConfigurationService.configurationUpdated(minimalContainerConfiguration())

      containerConfigurationService.getVia.isPresent shouldBe false
    }

    it("should return the configured via string") {
      val config = minimalContainerConfiguration()
      config.getDeploymentConfig.setVia("via")

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getVia.get shouldEqual "via"
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

    it("should return an un-patched deployment configuration is no patch matches the cluster ID") {
      val config = minimalContainerConfiguration()
      val clusterConfig = new DeploymentConfigurationPatch()
      val deploymentDirectoryPatch = new DeploymentDirectoryPatch()
      deploymentDirectoryPatch.setValue("/repose/patch/deployments")
      clusterConfig.setClusterId("foo-cluster-id")
      clusterConfig.setDeploymentDirectory(deploymentDirectoryPatch)
      config.getClusterConfig.add(clusterConfig)

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getDeploymentConfiguration should be theSameInstanceAs config.getDeploymentConfig
      containerConfigurationService.getDeploymentConfiguration.getDeploymentDirectory.getValue shouldEqual "/repose/deployments"
    }

    it("should return the patched deployment configuration") {
      val config = minimalContainerConfiguration()
      val clusterConfig = new DeploymentConfigurationPatch()
      val deploymentDirectoryPatch = new DeploymentDirectoryPatch()
      deploymentDirectoryPatch.setValue("/repose/patch/deployments")
      clusterConfig.setClusterId(DefaultClusterId)
      clusterConfig.setDeploymentDirectory(deploymentDirectoryPatch)
      config.getClusterConfig.add(clusterConfig)

      containerConfigurationService.configurationUpdated(config)

      containerConfigurationService.getDeploymentConfiguration should not be theSameInstanceAs(config.getDeploymentConfig)
      containerConfigurationService.getDeploymentConfiguration.getDeploymentDirectory.getValue shouldEqual "/repose/patch/deployments"
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