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

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.ResponseMode._
import org.openrepose.commons.utils.servlet.http.{HttpServletResponseWrapper, HttpServletRequestWrapper}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.addheader.config.AddHeadersConfig

import scala.collection.JavaConverters._

@Named
class AddHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[AddHeadersConfig] with LazyLogging {

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
    val requestWrapper: HttpServletRequestWrapper = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    val responseWrapper: HttpServletResponseWrapper = new HttpServletResponseWrapper(
      response.asInstanceOf[HttpServletResponse], MUTABLE, READONLY, response.getOutputStream)

    Option(config.getRequest).foreach { httpMessage =>
      httpMessage.getHeader.asScala.foreach { header =>
        if (header.isOverwrite) {
          requestWrapper.removeHeader(header.getName)
          logger.debug(s"Removing existing headers in request: ${header.getName}")
        }

        Option(header.getQuality) match {
          case Some(quality) => requestWrapper.addHeader(header.getName, header.getValue, header.getQuality)
          case None => requestWrapper.addHeader(header.getName, header.getValue)
        }
      }
    }

    chain.doFilter(requestWrapper, responseWrapper)

    Option(config.getResponse).foreach { httpMessage =>
      httpMessage.getHeader.asScala.foreach { header =>
        if (header.isOverwrite) {
          responseWrapper.removeHeader(header.getName)
          logger.debug(s"Removing existing headers in response: ${header.getName}")
        }

        Option(header.getQuality) match {
          case Some(quality) => responseWrapper.addHeader(header.getName, header.getValue, header.getQuality)
          case None => responseWrapper.addHeader(header.getName, header.getValue)
        }
      }
    }
    responseWrapper.commitToResponse()
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
