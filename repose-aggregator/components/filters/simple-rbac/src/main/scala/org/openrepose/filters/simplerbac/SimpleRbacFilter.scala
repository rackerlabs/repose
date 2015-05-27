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
package org.openrepose.filters.simplerbac

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.http.HttpServletResponse._

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.simplerbac.config.SimpleRbacConfig

@Named
class SimpleRbacFilter @Inject()(configurationService: ConfigurationService)
  extends Filter
  with UpdateListener[SimpleRbacConfig]
  with HttpDelegationManager
  with LazyLogging {

  private final val DEFAULT_CONFIG = "simple-rbac.cfg.xml"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: SimpleRbacConfig = _
  var initialized = false

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configurationFile)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/simple-rbac.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[SimpleRbacConfig]
    )
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("HERP filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
      val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

      logger.trace("HERP filter processing request...")
      mutableHttpResponse.setStatus(SC_NOT_IMPLEMENTED) // 501
      if (mutableHttpResponse.getStatus == SC_OK) {
        logger.trace("HERP filter passing request...")
        filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
        logger.trace("HERP filter handling response...")
      }
    }
    logger.trace("HERP filter returning response...")
  }

  override def configurationUpdated(configurationObject: SimpleRbacConfig): Unit = {
    configuration = configurationObject
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
