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

import com.fasterxml.jackson.core.JsonProcessingException
import com.typesafe.scalalogging.StrictLogging
import javax.inject.Named
import org.jtwig.environment.{EnvironmentConfiguration, EnvironmentConfigurationBuilder}
import org.jtwig.exceptions.JtwigException
import org.jtwig.value.Undefined
import org.jtwig.value.convert.string.{DefaultStringConverter, StringConverter}
import org.jtwig.{JtwigModel, JtwigTemplate}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.httplogging.HttpLoggingConfigListener._
import org.openrepose.core.services.httplogging.config.{Format, HttpLoggingConfig, Message}
import org.openrepose.core.services.httplogging.jtwig.HttpLoggingEnvironmentConfiguration
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

/**
  * Handles configuration for the [[HttpLoggingService]].
  */
@Named
class HttpLoggingConfigListener extends UpdateListener[HttpLoggingConfig] with StrictLogging {

  private var initialized: Boolean = false
  private var templates: List[LoggableTemplate] = List.empty

  override def configurationUpdated(configurationObject: HttpLoggingConfig): Unit = {
    logger.trace("Updating HTTP Logging Service configuration")

    templates = configurationObject.getMessage.asScala
      .toList
      .map { message =>
        val envConf = HttpLoggingEnvironmentConfiguration(message.getFormat)

        validateFormat(message, envConf).recover {
          case e: JtwigException =>
            // This type of Exception is known to be caused by our test data not matching the format of real data
            logger.warn("Unable to validate {} for {}", message.getFormat, message, e)
          case e: JsonProcessingException =>
            logger.warn("Failed to validate {} for {}", message.getFormat, message, e)
        }

        val template = JtwigTemplate.inlineTemplate(message.getValue, envConf)
        LoggableTemplate(template, LoggerFactory.getLogger(message.getLogTo))
      }

    initialized = true

    logger.trace("Updated HTTP Logging Service configuration")
  }

  override def isInitialized: Boolean = {
    initialized
  }

  def loggableTemplates: List[LoggableTemplate] = {
    templates
  }
}

object HttpLoggingConfigListener {
  private final val JsonValidationStringConverter: StringConverter = new DefaultStringConverter {
    override def convert(input: Any): String = input match {
      case Undefined.UNDEFINED => "123"
      case _ => super.convert(input)
    }
  }

  private def jsonValidationEnvConf(envConf: EnvironmentConfiguration): EnvironmentConfiguration = {
    // @formatter:off
    new EnvironmentConfigurationBuilder(envConf)
        .value()
          .withStringConverter(JsonValidationStringConverter)
        .and()
      .build()
    // @formatter:on
  }

  private def validateFormat(message: Message, envConf: EnvironmentConfiguration): Try[Unit] = {
    message.getFormat match {
      case Format.JSON =>
        Try {
          val emptyModel = JtwigModel.newModel()
          val template = JtwigTemplate.inlineTemplate(message.getValue, jsonValidationEnvConf(envConf))
          val rendering = template.render(emptyModel)
          Json.parse(rendering)
        }
      case _ =>
        Success(Unit)
    }
  }
}
