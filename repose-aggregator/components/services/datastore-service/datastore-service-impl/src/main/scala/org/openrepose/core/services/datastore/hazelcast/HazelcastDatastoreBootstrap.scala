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

import com.hazelcast.config.{Config, XmlConfigBuilder}
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
import org.openrepose.core.services.datastore.hazelcast.config.HazelcastDatastoreConfig
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy, Severity}
import org.openrepose.core.systemmodel.config.SystemModel

/**
  * Bootstraps a Hazelcast datastore.
  *
  * Monitors the system model for the inclusion of Hazelcast as a service.
  * If and only if the system model services list includes Hazelcast will a
  * Hazelcast datastore be created and registered.
  *
  * Configuration of the Hazelcast datastore will be determined by the
  * configuration of this service.
  * This service will template environment variables in any Hazelcast
  * configuration the same way that the configuration does.
  * This service supports live reloading of the configuration it relies on.
  * Since Hazelcast itself does not support live reloading, live reloading
  * requires restarting the Hazelcast instance.
  * Depending on the replication configuration, restarting the Hazelcast
  * instance may result in data loss.
  */
@Named
class HazelcastDatastoreBootstrap @Inject()(configurationService: ConfigurationService,
                                            healthCheckService: HealthCheckService,
                                            datastoreService: DatastoreService)
  extends StrictLogging {

  private final val xsdUrl = getClass.getResource("/META-INF/schema/config/hazelcast-datastore.xsd")

  private var healthCheckServiceProxy: HealthCheckServiceProxy = _

  @PostConstruct
  def init(): Unit = {
    logger.trace("Initializing Hazelcast Datastore Bootstrap")

    healthCheckServiceProxy = healthCheckService.register()
    configurationService.subscribeTo(
      SystemModelConfigFile,
      SystemModelConfigListener,
      classOf[SystemModel]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.trace("Destroying Hazelcast Datastore Bootstrap")

    configurationService.unsubscribeFrom(
      SystemModelConfigFile,
      SystemModelConfigListener
    )
    SystemModelConfigListener.unsubscribed()
  }

  private def refreshHazelcastConfig(config: Config): Unit = {
    // Set the Hazelcast logging type to match our own
    config.setProperty(GroupProperty.LOGGING_TYPE.getName, HazelcastLoggingTypeValue)

    // Idempotent -- noop if the datastore does not exist
    datastoreService.destroyDatastore(DatastoreName)

    datastoreService.createHazelcastDatastore(DatastoreName, config)
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
          HazelcastDatastoreConfigFile,
          xsdUrl,
          HazelcastDatastoreConfigListener,
          classOf[HazelcastDatastoreConfig]
        )
      } else if (!isEnabled && isRunning) {
        logger.debug("Disabling the Hazelcast datastore")

        configurationService.unsubscribeFrom(
          HazelcastDatastoreConfigFile,
          HazelcastDatastoreConfigListener
        )
        HazelcastDatastoreConfigListener.unsubscribed()
      }

      healthCheckServiceProxy.resolveIssue(NotConfiguredIssueName)
      initialized = true
    }

    override def isInitialized: Boolean = {
      initialized
    }

    // This method should only be called after this listener has been unsubscribed.
    // This method should clean up resources that this listener manages.
    def unsubscribed(): Unit = {
      configurationService.unsubscribeFrom(
        HazelcastDatastoreConfigFile,
        HazelcastDatastoreConfigListener
      )
      HazelcastDatastoreConfigListener.unsubscribed()
    }
  }

  object HazelcastDatastoreConfigListener extends UpdateListener[HazelcastDatastoreConfig] {
    private var initialized: Boolean = false

    // Tracks the currently monitored standard Hazelcast configuration so that we can
    // manage the listener.
    // Assumes that concurrency is not a concern (configuration updates are serialized).
    private var currentHref: Option[String] = None

    override def configurationUpdated(configurationObject: HazelcastDatastoreConfig): Unit = {
      logger.trace("Configuring Hazelcast")

      if (Option(configurationObject.getSimplified).nonEmpty) {
        logger.trace("Configuring Hazelcast with a simplified configuration")

        currentHref.foreach { href =>
          configurationService.unsubscribeFrom(
            href,
            HazelcastConfigListener
          )
        }
        currentHref = None

        val hazelcastConfig = HazelcastConfig.from(configurationObject.getSimplified)

        refreshHazelcastConfig(hazelcastConfig)
      } else {
        currentHref.filterNot(configurationObject.getStandard.getHref.equals)
          .foreach { href =>
            configurationService.unsubscribeFrom(
              href,
              HazelcastConfigListener
            )
          }
        currentHref = Some(configurationObject.getStandard.getHref)

        configurationService.subscribeTo(
          "",
          configurationObject.getStandard.getHref,
          HazelcastConfigListener,
          new TemplatingConfigurationParser(new InputStreamConfigurationParser())
        )
      }

      initialized = true
    }

    override def isInitialized: Boolean = {
      initialized
    }

    // This method should only be called after this listener has been unsubscribed.
    // This method should clean up resources that this listener manages.
    def unsubscribed(): Unit = {
      currentHref.foreach { href =>
        configurationService.unsubscribeFrom(
          href,
          HazelcastConfigListener
        )
      }

      datastoreService.destroyDatastore(DatastoreName)
    }
  }

  object HazelcastConfigListener extends UpdateListener[InputStream] {
    private var initialized: Boolean = false

    override def configurationUpdated(in: InputStream): Unit = {
      logger.trace("Configuring Hazelcast with a standard configuration")

      val hazelcastConfig = new XmlConfigBuilder(in)
        .setProperties(System.getProperties)
        .build()

      refreshHazelcastConfig(hazelcastConfig)

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
  final val HazelcastDatastoreConfigFile = s"$ServiceName.cfg.xml"
  final val NotConfiguredIssueName = "HazelcastDatastoreNotConfigured"

  private final val SystemModelConfigFile = "system-model.cfg.xml"
  private final val NotConfiguredMessage = "Hazelcast Datastore enabled but not configured"
  private final val HazelcastLoggingTypeValue = "slf4j"
}
