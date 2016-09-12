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
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.{ContainerConfiguration, DeploymentConfiguration}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConversions._

@Named
class ContainerConfigurationServiceImpl @Inject()(@Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                                  configurationService: ConfigurationService)
  extends ContainerConfigurationService with UpdateListener[ContainerConfiguration] with LazyLogging {

  import ContainerConfigurationServiceImpl._

  private val initialized = new AtomicBoolean(false)
  private val patchedDeploymentConfiguration = new AtomicReference[DeploymentConfiguration]()

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

  override def getVia: Optional[String] = {
    initializationCheck()
    Optional.ofNullable(patchedDeploymentConfiguration.get().getVia)
  }

  override def getContentBodyReadLimit: Optional[Long] = {
    initializationCheck()
    Optional.ofNullable(patchedDeploymentConfiguration.get().getContentBodyReadLimit)
  }

  override def getDeploymentConfiguration: DeploymentConfiguration = {
    initializationCheck()
    patchedDeploymentConfiguration.get()
  }

  override def isInitialized: Boolean =
    initialized.get()

  override def configurationUpdated(containerConfiguration: ContainerConfiguration): Unit = {
    val baseConfig = containerConfiguration.getDeploymentConfig

    patchedDeploymentConfiguration.set(containerConfiguration.getClusterConfig
      .find(patchConfig => clusterId.equals(patchConfig.getClusterId))
      .map(patchConfig => DeploymentConfigPatchUtil.patch(baseConfig, patchConfig))
      .getOrElse(baseConfig))

    initialized.set(true)
  }

  private def initializationCheck(): Unit =
    if (!isInitialized) throw new IllegalStateException(NotInitializedMessage)
}

object ContainerConfigurationServiceImpl {
  final val ContainerConfigurationFilename = "container.cfg.xml"
  final val NotInitializedMessage = "Service has not yet been initialized"
}
