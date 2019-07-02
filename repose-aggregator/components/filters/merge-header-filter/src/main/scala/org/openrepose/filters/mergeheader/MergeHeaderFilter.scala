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
package org.openrepose.filters.mergeheader

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService

import scala.collection.JavaConversions._

@Named
class MergeHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[MergeHeaderConfig] with StrictLogging {

  private final val DEFAULT_CONFIG = "merge-header.cfg.xml"

  private var initialized = false
  private var configFilename: String = _
  private var filterConfig: MergeHeaderConfig = _

  override def init(filterConfig: FilterConfig): Unit = {
    configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing MergeHeaderFilter using config $configFilename")
    val xsdURL = getClass.getResource("/META-INF/schema/config/merge-header.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configFilename,
      xsdURL,
      this,
      classOf[MergeHeaderConfig]
    )
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]

      Option(filterConfig.getRequest) foreach { requestConfig =>
        requestConfig.getHeader foreach { name =>
          val value = wrappedRequest.getHeaders(name).mkString(", ")
          if (value.nonEmpty) {
            logger.debug(s"REQUEST: merging header $name to $value")
            wrappedRequest.replaceHeader(name, value)
          }
        }
      }

      filterChain.doFilter(wrappedRequest, servletResponse)

      Option(filterConfig.getResponse) foreach { responseConfig =>
        responseConfig.getHeader foreach { name =>
          val value = httpServletResponse.getHeaders(name).mkString(", ")
          if (value.nonEmpty) {
            logger.debug(s"RESPONSE: merging header $name to $value")
            httpServletResponse.setHeader(name, value)
          }
        }
      }
    }
  }

  override def isInitialized: Boolean = initialized

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configFilename, this)
  }

  override def configurationUpdated(configurationObject: MergeHeaderConfig): Unit = {
    filterConfig = configurationObject
    initialized = true
  }
}
