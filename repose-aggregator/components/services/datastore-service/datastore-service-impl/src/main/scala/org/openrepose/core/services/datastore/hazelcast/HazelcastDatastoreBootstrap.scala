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
package org.openrepose.core.services.datastore.hazelcast

import com.hazelcast.config.{Config, UrlXmlConfig}
import com.hazelcast.spi.properties.GroupProperty
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.datastore.hazelcast.HazelcastDatastoreBootstrap._
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy, Severity}
import org.openrepose.core.systemmodel.config.SystemModel

/**
  * Bootstraps a Hazelcast datastore.
  *
  * Monitors the system model for the inclusion of Hazelcast as a service.
  * If and only if the system model services list includes Hazelcast will a
  * Hazelcast datastore be created and registered.
  *
  * Configuration of the Hazelcast datastore will be determined by a Hazelcast
  * XML configuration file in the Repose configuration directory.
  * Note that live updates of Hazelcast configuration are not supported;
  * Hazelcast itself does not trivially support live updates, so supporting them
  * here would require restarting Hazelcast which may cause data to be lost.
  * To force Hazelcast configuration to reloaded, the Hazelcast datastore
  * service can be removed and re-added to the system model, or Repose can
  * be restarted.
  */
@Named
class HazelcastDatastoreBootstrap @Inject()(configurationService: ConfigurationService,
                                            healthCheckService: HealthCheckService,
                                            datastoreService: DatastoreService)
  extends UpdateListener[SystemModel] with StrictLogging {

  private var initialized: Boolean = false
  private var healthCheckServiceProxy: HealthCheckServiceProxy = _

  @PostConstruct
  def init(): Unit = {
    logger.trace("Initializing Hazelcast Datastore Bootstrap")

    healthCheckServiceProxy = healthCheckService.register()
    configurationService.subscribeTo(
      SystemModelConfig,
      this,
      classOf[SystemModel]
    )
  }

  override def configurationUpdated(configurationObject: SystemModel): Unit = {
    logger.trace("Inspecting system model for inclusion of the Hazelcast datastore service")

    val isEnabled = SystemModelInterrogator.getService(configurationObject, ServiceName).isPresent
    val isRunning = Option(datastoreService.getDatastore(DatastoreName)).isDefined

    if (isEnabled && !isRunning) {
      logger.debug("Enabling the Hazelcast datastore")

      healthCheckServiceProxy.reportIssue(
        NotConfiguredIssueName,
        NotConfiguredMessage,
        Severity.BROKEN
      )

      datastoreService.createHazelcastDatastore(DatastoreName, hazelcastConfig)
    } else if (!isEnabled && isRunning) {
      logger.debug("Disabling the Hazelcast datastore")

      datastoreService.destroyDatastore(DatastoreName)
    }

    healthCheckServiceProxy.resolveIssue(NotConfiguredIssueName)
    initialized = true
  }

  override def isInitialized: Boolean = {
    initialized
  }

  private def hazelcastConfig: Config = {
    logger.trace("Resolving Hazelcast configuration")
    val resourceResolver = configurationService.getResourceResolver
    val configUrl = resourceResolver.resolve(HazelcastConfig).name
    new UrlXmlConfig(configUrl)
      .setProperty(GroupProperty.LOGGING_TYPE.getName, HazelcastLoggingTypeValue)
  }
}

object HazelcastDatastoreBootstrap {
  final val DatastoreName = "hazelcast"
  final val ServiceName = s"$DatastoreName-datastore"
  final val NotConfiguredIssueName = "HazelcastDatastoreNotConfigured"

  private final val SystemModelConfig = "system-model.cfg.xml"
  private final val HazelcastConfig = "hazelcast.xml"
  private final val NotConfiguredMessage = "Hazelcast Datastore enabled but not configured"
  private final val HazelcastLoggingTypeValue = "slf4j"
}
