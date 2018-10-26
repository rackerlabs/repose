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

import java.lang.Long
import java.util.Optional
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.{ContainerConfiguration, DeploymentConfiguration}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConversions._

@Named
class ContainerConfigurationServiceImpl @Inject()(@Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                                  configurationService: ConfigurationService)
  extends ContainerConfigurationService with UpdateListener[ContainerConfiguration] with StrictLogging {

  import ContainerConfigurationServiceImpl._

  private var initialized: Boolean = false
  private var patchedDeploymentConfiguration: DeploymentConfiguration = _
  private var updateListeners: Set[UpdateListener[DeploymentConfiguration]] = Set.empty

  @PostConstruct
  def init(): Unit = {
    logger.debug("Initializing service and registering configuration listener")
    val xsdUrl = getClass.getResource("/META-INF/schema/container/container-configuration.xsd")

    configurationService.subscribeTo(
      ContainerConfigurationFilename,
      xsdUrl,
      this,
      classOf[ContainerConfiguration]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.debug("Un-registering configuration listener and shutting down service")
    configurationService.unsubscribeFrom(ContainerConfigurationFilename, this)
  }

  override def getRequestVia: Optional[String] = {
    initializationCheck()
    Optional ofNullable {
      Option(patchedDeploymentConfiguration.getViaHeader)
        .map(_.getRequestPrefix)
        .orNull
    }
  }

  override def getResponseVia: Optional[String] = {
    initializationCheck()
    Optional ofNullable {
      Option(patchedDeploymentConfiguration.getViaHeader)
        .map(_.getResponsePrefix)
        .orNull
    }
  }

  override def includeViaReposeVersion: Boolean = {
    initializationCheck()
    Option(patchedDeploymentConfiguration.getViaHeader).forall(_.isReposeVersion)
  }

  override def getContentBodyReadLimit: Optional[Long] = {
    initializationCheck()
    Optional.ofNullable(patchedDeploymentConfiguration.getContentBodyReadLimit)
  }

  override def getDeploymentConfiguration: DeploymentConfiguration = {
    initializationCheck()
    patchedDeploymentConfiguration
  }

  override def subscribeTo(listener: UpdateListener[DeploymentConfiguration]): Unit = {
    subscribeTo(listener, sendNotificationNow = true)
  }

  override def subscribeTo(listener: UpdateListener[DeploymentConfiguration], sendNotificationNow: Boolean): Unit = {
    initializationCheck()
    logger.debug("Subscribing listener with hash code: {}", listener.hashCode.toString)

    // TODO: Clean this up with Scala 2.12 support for Scala function -> Java 8 function
    updateListeners = updateListeners + listener

    if (sendNotificationNow) listener.configurationUpdated(patchedDeploymentConfiguration)
  }

  override def unsubscribeFrom(listener: UpdateListener[DeploymentConfiguration]): Unit = {
    initializationCheck()
    logger.debug("Unsubscribing listener with hash code: {}", listener.hashCode.toString)

    updateListeners = updateListeners - listener
  }

  override def isInitialized: Boolean =
    initialized

  override def configurationUpdated(containerConfiguration: ContainerConfiguration): Unit = {
    val baseConfig = containerConfiguration.getDeploymentConfig
    val patchedDeploymentConfig = containerConfiguration.getClusterConfig
      .find(patchConfig => clusterId.equals(patchConfig.getClusterId))
      .map(patchConfig => DeploymentConfigPatchUtil.patch(baseConfig, patchConfig))
      .getOrElse(baseConfig)

    patchedDeploymentConfiguration = patchedDeploymentConfig
    initialized = true

    updateListeners foreach { listener =>
      try {
        listener.configurationUpdated(patchedDeploymentConfig)
      } catch {
        case ex: Exception =>
          logger.error("Configuration update error. Reason: {}", ex.getLocalizedMessage)
          logger.trace("", ex)
      }
    }
  }

  // Note: The container configuration should be read on init of this class. As such, this check
  //       should not ever fail.
  private def initializationCheck(): Unit = {
    if (!isInitialized) {
      logger.error(NotInitializedMessage)
      throw new IllegalStateException(NotInitializedMessage)
    }
  }
}

object ContainerConfigurationServiceImpl {
  final val ContainerConfigurationFilename = "container.cfg.xml"
  final val NotInitializedMessage = "The ContainerConfigurationService has not yet been initialized"
}
