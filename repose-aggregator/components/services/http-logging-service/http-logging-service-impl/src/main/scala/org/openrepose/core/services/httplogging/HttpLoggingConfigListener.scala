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
import javax.inject.Named
import org.jtwig.environment.{EnvironmentConfiguration, EnvironmentConfigurationBuilder}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.httplogging.config.{Format, HttpLoggingConfig, Message}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

@Named
class HttpLoggingConfigListener extends UpdateListener[HttpLoggingConfig] with StrictLogging {

  import HttpLoggingConfigListener._

  private var initialized: Boolean = false
  private var templates: List[Template] = List.empty

  override def configurationUpdated(configurationObject: HttpLoggingConfig): Unit = {
    logger.trace("Updating HTTP Logging Service configuration")

    templates = configurationObject.getMessage.asScala
      .toList
      .map { message =>
        val logger = LoggerFactory.getLogger(message.getLogTo)
        val envConf = (setInitialEscapeEngine(message.getFormat)(_))
          .apply(defaultEnvConfBuilder)
          .build()
        Template(message, envConf, logger)
      }

    initialized = true

    logger.trace("Updated HTTP Logging Service configuration")
  }

  override def isInitialized: Boolean = {
    initialized
  }

  def currentTemplates: List[Template] = {
    templates
  }
}

object HttpLoggingConfigListener {

  case class Template(message: Message, envConf: EnvironmentConfiguration, logger: Logger)

  private final val StartCodeTag: String = "{;"
  private final val EndCodeTag: String = ";}"
  private final val JsonEscapeEngineName: String = "javascript"
  private final val NoopEscapeEngineName: String = "none"
  private final val EscapeEnginesByFormat: Map[Format, String] = Map(
    Format.JSON -> JsonEscapeEngineName
  )

  private def defaultEnvConfBuilder: EnvironmentConfigurationBuilder = {
    // @formatter:off
    EnvironmentConfigurationBuilder
      .configuration()
        .parser()
          .syntax()
            .withStartCode(StartCodeTag).withEndCode(EndCodeTag)
          .and()
        .and()
        .render()
          .withStrictMode(true)
        .and()
    // @formatter:on
  }

  private def setInitialEscapeEngine(format: Format)(envConfBuilder: EnvironmentConfigurationBuilder): EnvironmentConfigurationBuilder = {
    val initialEscapeEngine = EscapeEnginesByFormat.getOrElse(format, NoopEscapeEngineName)

    // @formatter:off
    envConfBuilder
      .escape()
        .withInitialEngine(initialEscapeEngine)
      .and()
    // @formatter:on
  }
}
