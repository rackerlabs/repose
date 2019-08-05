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
package org.openrepose.core.services.uriredaction

import com.typesafe.scalalogging.StrictLogging
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.uriredaction.config.UriRedactionConfig

import scala.collection.JavaConverters._
import scala.util.matching.Regex

/**
  * A service which redacts sensitive data as defined by RegEx's in the config.
  */
@Named
class UriRedactionServiceImpl @Inject()(configurationService: ConfigurationService)
  extends UriRedactionService with UpdateListener[UriRedactionConfig] with StrictLogging {

  private var redactions = Seq.empty[Regex]
  private var initialized = false

  import org.openrepose.core.services.uriredaction.UriRedactionServiceImpl._

  @PostConstruct
  def init(): Unit = {
    logger.info("Initializing and registering configuration listener")
    val xsdURL = getClass.getResource("/META-INF/schema/config/uri-redaction.xsd")

    configurationService.subscribeTo(
      DefaultConfig,
      xsdURL,
      this,
      classOf[UriRedactionConfig]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.info("Unsubscribing configuration listener and shutting down service")
    configurationService.unsubscribeFrom(DefaultConfig, this)
  }

  override def configurationUpdated(configurationObject: UriRedactionConfig): Unit = {
    synchronized {
      logger.trace("Service configuration updated")
      redactions = Option(configurationObject.getRedact)
        .map(_.asScala).getOrElse(List.empty[String])
        .map(_.r)
      initialized = true
    }
  }

  override def isInitialized: Boolean = initialized

  override def redact(uriString: String): String = {
    logger.trace("Redacting: {}", uriString)
    val rtn = redactions.foldLeft(uriString) { (newUri, redaction) =>
      val matcher = redaction.pattern.matcher(newUri)
      if (matcher.matches) {
        (matcher.groupCount to 1 by -1).foldLeft(newUri)((redactedUri, groupIndex) =>
          s"${redactedUri.substring(0, matcher.start(groupIndex))}$RedactedString${redactedUri.substring(matcher.end(groupIndex))}")
      } else {
        newUri
      }
    }
    logger.trace("To: {}", rtn)
    rtn
  }
}

object UriRedactionServiceImpl {
  final val ServiceName = "uri-redaction"
  final val RedactedString = "XXXXX"
  private final val DefaultConfig = ServiceName + ".cfg.xml"
}
