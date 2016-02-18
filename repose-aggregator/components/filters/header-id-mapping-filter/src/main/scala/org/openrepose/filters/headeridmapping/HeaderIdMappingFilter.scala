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

package org.openrepose.filters.headeridmapping

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.headeridmapping.config.{HttpHeader, HeaderIdMappingConfig}

import scala.collection.JavaConverters._

@Named
class HeaderIdMappingFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[HeaderIdMappingConfig] with LazyLogging {

  import HeaderIdMappingFilter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var configuredHeaders: Iterable[HttpHeader] = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Header Identity Mapping filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing Header Identity Mapping filter using config $configurationFile")
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[HeaderIdMappingConfig])

    logger.trace("Header Identity Mapping filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.warn("Header Identity Mapping filter has not yet initialized.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
      return
    }

    val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

    // find the first configured "user-header" that exists in the request and use that config section to populate the user/group headers
    configuredHeaders.toStream.flatMap(extractUserGroup(wrappedRequest, _)).headOption.foreach { userGroupValues =>
      wrappedRequest.addHeader(PowerApiHeader.USER.toString, userGroupValues.user, userGroupValues.quality)
      userGroupValues.group.foreach(wrappedRequest.addHeader(PowerApiHeader.GROUPS.toString, _, userGroupValues.quality))
    }

    filterChain.doFilter(wrappedRequest, servletResponse)
  }

  private def extractUserGroup(request: HttpServletRequestWrapper, configuredHeader: HttpHeader): Option[UserGroupValues] =
    getFirstHeaderValue(request, configuredHeader.getUserHeader)
      .map(UserGroupValues(_, getFirstHeaderValue(request, configuredHeader.getGroupHeader), configuredHeader.getQuality))

  // carry-over behavior from the old filter - be sure we only get the first value of the header
  def getFirstHeaderValue(request: HttpServletRequestWrapper, header: String): Option[String] =
    request.getSplittableHeaderScala(header).headOption.map(_.trim).filterNot(_.isEmpty)

  override def destroy(): Unit = {
    logger.trace("Header Identity Mapping filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Header Identity Mapping filter destroyed.")
  }

  override def configurationUpdated(config: HeaderIdMappingConfig): Unit = {
    configuredHeaders = config.getSourceHeaders.getHeader.asScala
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object HeaderIdMappingFilter {
  private final val DEFAULT_CONFIG = "header-id-mapping.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/header-id-mapping-configuration.xsd"

  case class UserGroupValues(user: String, group: Option[String], quality: Double)
}