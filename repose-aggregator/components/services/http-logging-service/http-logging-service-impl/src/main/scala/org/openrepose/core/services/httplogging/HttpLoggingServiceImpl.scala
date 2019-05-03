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
package org.openrepose.core.services.httplogging

import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}
import org.jtwig.JtwigModel
import org.jtwig.environment.EnvironmentConfigurationBuilder
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httplogging.HttpLoggingServiceImpl._
import org.openrepose.core.services.httplogging.config.HttpLoggingConfig

@Named
class HttpLoggingServiceImpl @Inject()(configurationService: ConfigurationService)
  extends HttpLoggingService with StrictLogging {

  private var configuration: HttpLoggingConfig = _

  @PostConstruct
  def init(): Unit = {
    logger.trace("Initializing HTTP Logging Service")

    val defaultConfigSchemaUrl = classOf[HttpLoggingServiceImpl].getResource(DefaultConfigSchema)
    configurationService.subscribeTo(
      DefaultConfig,
      defaultConfigSchemaUrl,
      ConfigurationListener,
      classOf[HttpLoggingConfig])

    logger.trace("Initialized HTTP Logging Service")
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.trace("Destroying HTTP Logging Service")

    configurationService.unsubscribeFrom(DefaultConfig, ConfigurationListener)

    logger.trace("Destroyed HTTP Logging Service")
  }

  override def open(): HttpLoggingContext = {
    val context = new HttpLoggingContext()
    logger.trace("Opened a new HTTP logging context {}", s"${context.hashCode()}")
    context
  }

  override def close(httpLoggingContext: HttpLoggingContext): Unit = {
    ???
  }

  private object ConfigurationListener extends UpdateListener[HttpLoggingConfig] {
    private var initialized: Boolean = false

    override def configurationUpdated(configurationObject: HttpLoggingConfig): Unit = {
      logger.trace("Updating HTTP Logging Service configuration")

      configuration = configurationObject
      initialized = true

      logger.trace("Updated HTTP Logging Service configuration")
    }

    override def isInitialized: Boolean = {
      initialized
    }
  }

}

object HttpLoggingServiceImpl {
  final val ServiceName: String = "http-logging-service"
  final val DefaultConfig: String = "http-logging.cfg.xml"
  final val DefaultConfigSchema: String = "/META-INF/schema/config/http-logging.xsd"

}
