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
package org.openrepose.filters.rackspaceauthuser

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService

@Named
class RackspaceAuthUserFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[RackspaceAuthUserConfig] with LazyLogging {

  import RackspaceAuthUserFilter._

  private var configFileName: String = _
  private val initialized = new AtomicBoolean(false)
  private val handler: AtomicReference[RackspaceAuthUserHandler] = new AtomicReference[RackspaceAuthUserHandler]()

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Rackspace Auth User filter initializing...")
    configFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing Rackspace Auth User filter using config $configFileName")
    val xsdURL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configFileName, xsdURL, this, classOf[RackspaceAuthUserConfig])

    logger.trace("Rackspace Auth User filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
      if (httpServletRequest.getMethod != "POST") {
        // this filter only operates on POST requests that have a body to parse
        filterChain.doFilter(servletRequest, servletResponse)
      } else {
        val wrappedRequest = new HttpServletRequestWrapper(httpServletRequest)
        handler.get.parseUserGroupFromInputStream(wrappedRequest.getInputStream, wrappedRequest.getContentType) foreach { rackspaceAuthUserGroup =>
          rackspaceAuthUserGroup.domain.foreach { domainVal =>
            wrappedRequest.addHeader(PowerApiHeader.DOMAIN.toString, domainVal)
          }
          wrappedRequest.addHeader(PowerApiHeader.USER.toString, rackspaceAuthUserGroup.user, rackspaceAuthUserGroup.quality)
          wrappedRequest.addHeader(PowerApiHeader.GROUPS.toString, rackspaceAuthUserGroup.group, rackspaceAuthUserGroup.quality)
        }

        filterChain.doFilter(wrappedRequest, servletResponse)
      }
    }
  }

  override def destroy(): Unit = {
    logger.trace("Rackspace Auth User filter destroying...")
    configurationService.unsubscribeFrom(configFileName, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Rackspace Auth User filter destroyed.")
  }

  override def configurationUpdated(config: RackspaceAuthUserConfig): Unit = {
    handler.set(new RackspaceAuthUserHandler(config))
    initialized.set(true)
  }

  override def isInitialized: Boolean = initialized.get
}

object RackspaceAuthUserFilter {
  private final val DEFAULT_CONFIG = "rackspace-auth-user.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/config/schema/rackspace-auth-user-configuration.xsd"
}
