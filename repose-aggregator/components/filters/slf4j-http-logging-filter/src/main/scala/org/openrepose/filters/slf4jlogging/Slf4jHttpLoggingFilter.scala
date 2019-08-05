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
package org.openrepose.filters.slf4jlogging

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.logging.apache.HttpLogFormatter
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.slf4jlogging.config.Slf4JHttpLoggingConfig
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._
import scala.collection.mutable

@Named
class Slf4jHttpLoggingFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[Slf4JHttpLoggingConfig] with StrictLogging {

  private final val DEFAULT_CONFIG: String = "slf4j-http-logging.cfg.xml"

  private var initialized: Boolean = false
  private var loggerWrappers: Seq[Slf4jLoggerWrapper] = Seq.empty
  private var configFilename: String = _

  override def init(filterConfig: FilterConfig): Unit = {
    configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing SLF4JHttpLogging filter using config " + configFilename)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/slf4j-http-logging-configuration.xsd")
    configurationService.subscribeTo(filterConfig.getFilterName,
      configFilename,
      xsdURL,
      this,
      classOf[Slf4JHttpLoggingConfig])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
      val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]

      filterChain.doFilter(httpServletRequest, httpServletResponse)

      loggerWrappers foreach { loggerWrapper =>
        val formattedMessage = loggerWrapper.formatter.format(httpServletRequest, httpServletResponse)
        loggerWrapper.logger.info(formattedMessage)
      }
    }
  }

  override def isInitialized: Boolean = initialized

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configFilename, this)
  }

  override def configurationUpdated(configurationObject: Slf4JHttpLoggingConfig): Unit = {
    synchronized {
      val newLoggerWrappers = mutable.ListBuffer.empty[Slf4jLoggerWrapper]

      configurationObject.getSlf4JHttpLog foreach { logConfig =>
        val loggerName = logConfig.getId
        val formatString = (Option(logConfig.getFormat), Option(logConfig.getFormatElement)) match {
          case (Some(format), _) =>
            format
          case (None, Some(formatElement)) =>
            var format = formatElement.getValue.trim
            if (formatElement.isCrush) {
              format = format.replaceAll("(?m)[ \\t]*(\\r\\n|\\r|\\n)[ \\t]*", " ")
            }
            format
          case (None, None) =>
            throw new IllegalArgumentException("Either the format element or the format attribute must be defined")
        }

        newLoggerWrappers += new Slf4jLoggerWrapper(LoggerFactory.getLogger(loggerName), formatString)
      }

      loggerWrappers = newLoggerWrappers
      initialized = true
    }
  }

  case class Slf4jLoggerWrapper(logger: Logger, formatString: String) {
    val formatter = new HttpLogFormatter(formatString)
  }

}
