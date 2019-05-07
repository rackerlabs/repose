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
import org.jtwig.{JtwigModel, JtwigTemplate}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httplogging.HttpLoggingConfigListener.Template
import org.openrepose.core.services.httplogging.config.HttpLoggingConfig

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Named
class HttpLoggingServiceImpl @Inject()(configurationService: ConfigurationService,
                                       httpLoggingConfigListener: HttpLoggingConfigListener)
  extends HttpLoggingService with StrictLogging {

  import HttpLoggingServiceImpl._

  @PostConstruct
  def init(): Unit = {
    logger.trace("Initializing HTTP Logging Service")

    val defaultConfigSchemaUrl = classOf[HttpLoggingServiceImpl].getResource(DefaultConfigSchema)
    configurationService.subscribeTo(
      DefaultConfig,
      defaultConfigSchemaUrl,
      httpLoggingConfigListener,
      classOf[HttpLoggingConfig])

    logger.trace("Initialized HTTP Logging Service")
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.trace("Destroying HTTP Logging Service")

    configurationService.unsubscribeFrom(DefaultConfig, httpLoggingConfigListener)

    logger.trace("Destroyed HTTP Logging Service")
  }

  override def open(): HttpLoggingContext = {
    val context = new HttpLoggingContext()
    logger.trace("Opened a new HTTP logging context {}", s"${context.hashCode()}")
    context
  }

  override def close(httpLoggingContext: HttpLoggingContext): Unit = {
    logger.trace("Closing the HTTP logging context {}", s"${httpLoggingContext.hashCode()}")
    httpLoggingConfigListener.currentTemplates.foreach { template =>
      Future {
        val contextMap = HttpLoggingContextMap.from(httpLoggingContext)
        val renderedMessage = renderTemplate(template, contextMap)
        template.logger.info(renderedMessage)
        logger.trace(
          "Logged message {} for HTTP logging context {}",
          s"${template.message.hashCode()}",
          s"${httpLoggingContext.hashCode()}"
        )
      }
    }
  }
}

object HttpLoggingServiceImpl {
  final val DefaultConfig: String = "http-logging.cfg.xml"

  private final val DefaultConfigSchema: String = "/META-INF/schema/config/http-logging.xsd"

  private def renderTemplate(template: Template, context: Map[String, AnyRef]): String = {
    val model = JtwigModel.newModel(context.asJava)

    JtwigTemplate
      .inlineTemplate(template.message.getValue, template.envConf)
      .render(model)
  }
}
