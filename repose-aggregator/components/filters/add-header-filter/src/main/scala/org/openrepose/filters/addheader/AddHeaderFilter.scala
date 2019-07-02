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
package org.openrepose.filters.addheader

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.ResponseMode._
import org.openrepose.commons.utils.servlet.http.{HeaderInteractor, HttpServletResponseWrapper, HttpServletRequestWrapper}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.addheader.config.{HttpMessage, AddHeadersConfig}

import scala.collection.JavaConverters._

@Named
class AddHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[AddHeadersConfig] with StrictLogging {

  import AddHeaderFilter._

  private var configurationFile: String = _
  private var config: AddHeadersConfig = _
  private var initialized = false

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Add Header filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG_NAME)
    logger.info(s"Initializing Add Header filter using config $configurationFile")
    val xsdURL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdURL, this, classOf[AddHeadersConfig])
    logger.trace("Add Header filter initialized.")
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val requestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])

      val responseWrapper: HttpServletResponseWrapper = new HttpServletResponseWrapper(
        response.asInstanceOf[HttpServletResponse], MUTABLE, READONLY, response.getOutputStream)

      addHeaders(requestWrapper, config.getRequest)

      chain.doFilter(requestWrapper, responseWrapper)

      addHeaders(responseWrapper, config.getResponse)
      responseWrapper.commitToResponse()
    }
  }

  def addHeaders(wrapper: HeaderInteractor, configuredHeaders: HttpMessage): Unit = {
    Option(configuredHeaders).foreach { httpMessage =>
      httpMessage.getHeader.asScala.foreach { header =>
        if (header.isOverwrite) {
          wrapper.removeHeader(header.getName)
          logger.debug(s"Removing existing headers: ${header.getName}")
        }

        Option(header.getQuality) match {
          case Some(quality) => wrapper.addHeader(header.getName, header.getValue, header.getQuality)
          case None => wrapper.addHeader(header.getName, header.getValue)
        }
      }
    }
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
  }

  override def configurationUpdated(newConfig: AddHeadersConfig): Unit = {
    config = newConfig
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object AddHeaderFilter {
  private final val DEFAULT_CONFIG_NAME = "add-header.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/add-header.xsd"
}
