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

package org.openrepose.filters.uriuser

import java.net.URL
import java.util.regex.Pattern
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.uriuser.config.UriUserConfig

import scala.collection.JavaConversions._

@Named
class UriUserFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[UriUserConfig] with StrictLogging {

  import UriUserFilter._

  private var configurationFileName: String = DefaultConfigFileName
  private var initialized = false
  private var config: Config = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("URI User filter initializing...")
    configurationFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfigFileName)

    logger.info(s"Initializing URI User Filter using config $configurationFileName")
    val xsdUrl: URL = getClass.getResource(SchemaFileName)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFileName, xsdUrl, this, classOf[UriUserConfig])

    logger.trace("URI User filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

      config.uriRegex.toStream
        .map(_.matcher(wrappedRequest.getRequestURI))
        .find(m => m.find() && m.groupCount > 0)
        .map(_.group(1)) foreach { user =>
          wrappedRequest.addHeader(PowerApiHeader.USER, user, config.quality)
          wrappedRequest.addHeader(PowerApiHeader.GROUPS, config.group, config.quality)
      }

      filterChain.doFilter(wrappedRequest, servletResponse)
    }
  }

  override def destroy(): Unit = {
    logger.trace("URI User filter destroying...")
    configurationService.unsubscribeFrom(configurationFileName, this.asInstanceOf[UpdateListener[_]])
    logger.trace("URI User filter destroyed.")
  }

  override def configurationUpdated(uriUserConfig: UriUserConfig): Unit = {
    config = Config(
      uriUserConfig.getIdentificationMappings.getMapping.map(_.getIdentificationRegex.r.pattern),
      Option(uriUserConfig.getGroup).filterNot(_.trim.isEmpty).getOrElse(DefaultGroup),
      Option(uriUserConfig.getQuality).map(_.toDouble).getOrElse(DefaultQuality))
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object UriUserFilter {
  private final val DefaultConfigFileName = "uri-user.cfg.xml"
  private final val SchemaFileName = "/META-INF/schema/config/uri-user-configuration.xsd"

  // while the XSD specifies these defaults, the generated getters don't provide these values for some reason
  private final val DefaultQuality = 0.5d
  private final val DefaultGroup = "User_Standard"

  case class Config(uriRegex: Seq[Pattern], group: String, quality: Double)
}
