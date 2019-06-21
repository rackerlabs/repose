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

import java.io.InputStream

import com.hazelcast.config.XmlConfigBuilder
import com.hazelcast.spi.properties.GroupProperty
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.config.parser.common.TemplatingConfigurationParser
import org.openrepose.commons.config.parser.inputstream.InputStreamConfigurationParser
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
  extends StrictLogging {

  private var healthCheckServiceProxy: HealthCheckServiceProxy = _

  @PostConstruct
  def init(): Unit = {
    logger.trace("Initializing Hazelcast Datastore Bootstrap")

    healthCheckServiceProxy = healthCheckService.register()
    configurationService.subscribeTo(
      SystemModelConfig,
      SystemModelConfigListener,
      classOf[SystemModel]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.trace("Destroying Hazelcast Datastore Bootstrap")

    configurationService.unsubscribeFrom(
      SystemModelConfig,
      SystemModelConfigListener
    )
  }

  object SystemModelConfigListener extends UpdateListener[SystemModel] {
    private var initialized: Boolean = false

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

        configurationService.subscribeTo(
          "",
          HazelcastConfig,
          HazelcastDatastoreConfigListener,
          new TemplatingConfigurationParser(new InputStreamConfigurationParser())
        )
      } else if (!isEnabled && isRunning) {
        logger.debug("Disabling the Hazelcast datastore")

        configurationService.unsubscribeFrom(
          HazelcastConfig,
          HazelcastDatastoreConfigListener
        )

        datastoreService.destroyDatastore(DatastoreName)
      }

      healthCheckServiceProxy.resolveIssue(NotConfiguredIssueName)
      initialized = true
    }

    override def isInitialized: Boolean = {
      initialized
    }
  }

  object HazelcastDatastoreConfigListener extends UpdateListener[InputStream] {
    private var initialized: Boolean = false

    override def configurationUpdated(in: InputStream): Unit = {
      logger.trace("Creating Hazelcast configuration")

      val isRunning = Option(datastoreService.getDatastore(DatastoreName)).isDefined

      if (isRunning) {
        datastoreService.destroyDatastore(DatastoreName)
      }

      val hazelcastConfig = new XmlConfigBuilder(in)
        .setProperties(System.getProperties)
        .build()
        .setProperty(GroupProperty.LOGGING_TYPE.getName, HazelcastLoggingTypeValue)

      datastoreService.createHazelcastDatastore(DatastoreName, hazelcastConfig)

      initialized = true
    }

    override def isInitialized: Boolean = {
      initialized
    }
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
