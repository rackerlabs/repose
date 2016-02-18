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
package org.openrepose.filters.headertranslation

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.headertranslation.config.{Header, HeaderTranslationType}

import scala.collection.JavaConversions._

@Named
class HeaderTranslationFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[HeaderTranslationType] with LazyLogging {

  private final val DEFAULT_CONFIG = "header-translation.cfg.xml"

  private var configFilename: String = _
  private var initialized: Boolean = false
  private var sourceHeaders: List[Header] = List.empty

  override def init(filterConfig: FilterConfig): Unit = {
    configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configFilename)

    val xsdURL = getClass.getResource("/META-INF/schema/config/header-translation.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configFilename,
      xsdURL,
      this,
      classOf[HeaderTranslationType]
    )
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized...")
      response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])

      sourceHeaders foreach { sourceHeader =>
        val originalHeaderName = sourceHeader.getOriginalName
        val originalHeaderValues = httpRequest.getHeadersScala(originalHeaderName)

        if (originalHeaderValues.nonEmpty) {
          sourceHeader.getNewName foreach { newHeaderName =>
            originalHeaderValues foreach { originalHeaderValue =>
              httpRequest.addHeader(newHeaderName, originalHeaderValue)
            }
            logger.trace("Header added: " + newHeaderName)
          }

          if (sourceHeader.isRemoveOriginal) {
            httpRequest.removeHeader(originalHeaderName)
            logger.trace("Header removed: " + originalHeaderName)
          }
        } else {
          logger.trace("Header for translation not found: " + originalHeaderName)
        }
      }

      chain.doFilter(httpRequest, response)
    }
  }

  override def isInitialized: Boolean = {
    initialized
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configFilename, this)
  }

  override def configurationUpdated(configurationObject: HeaderTranslationType): Unit = {
    sourceHeaders = configurationObject.getHeader.toList
    initialized = true
  }
}
