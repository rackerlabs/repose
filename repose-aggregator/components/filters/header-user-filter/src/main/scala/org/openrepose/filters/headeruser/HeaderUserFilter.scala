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
package org.openrepose.filters.headeruser

import java.util.concurrent.atomic.AtomicReference
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.headeruser.config.HeaderUserConfig

import javax.inject.Inject
import javax.inject.Named
import javax.servlet._
import java.net.URL

import collection.JavaConversions._

@Named
class HeaderUserFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[HeaderUserConfig] with StrictLogging {

  import HeaderUserFilter._

  private var configFileName: String = _
  private var initialized = false
  private val configuredHeaders: AtomicReference[List[ConfiguredHeader]] = new AtomicReference[List[ConfiguredHeader]]()

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Header User filter initializing...")
    configFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfig)

    logger.info(s"Initializing Header User filter using config $configFileName")
    val xsdUrl: URL = getClass.getResource(SchemaFileName)
    configurationService.subscribeTo(filterConfig.getFilterName, configFileName, xsdUrl, this, classOf[HeaderUserConfig])

    logger.trace("Header User filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
      return
    }

    val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

    configuredHeaders.get flatMap { configuredHeader =>
      wrappedRequest.getSplittableHeaderScala(configuredHeader.name).headOption
        .map(_.trim)
        .filterNot(_.isEmpty)
        .map(RequestHeader(configuredHeader.name, _, configuredHeader.quality))
    } foreach { header =>
      wrappedRequest.addHeader(PowerApiHeader.USER, header.value, header.quality)
      wrappedRequest.addHeader(PowerApiHeader.GROUPS, header.name, header.quality)
    }

    filterChain.doFilter(wrappedRequest, servletResponse)
  }

  override def configurationUpdated(config: HeaderUserConfig): Unit = {
    configuredHeaders.set(
      config.getSourceHeaders.getHeader.map(header => ConfiguredHeader(header.getId, header.getQuality)).toList)
    initialized = true
  }

  override def destroy(): Unit = {
    logger.trace("Header User filter destroying...")
    configurationService.unsubscribeFrom(configFileName, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Header User filter destroyed.")
  }

  override def isInitialized: Boolean = initialized
}

object HeaderUserFilter {
  private final val DefaultConfig = "header-user.cfg.xml"
  private final val SchemaFileName = "/META-INF/schema/config/header-user-configuration.xsd"

  case class ConfiguredHeader(name: String, quality: Double)
  case class RequestHeader(name: String, value: String, quality: Double)
}
