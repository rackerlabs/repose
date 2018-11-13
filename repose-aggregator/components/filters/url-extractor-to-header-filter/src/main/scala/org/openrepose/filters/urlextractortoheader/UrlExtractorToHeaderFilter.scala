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
package org.openrepose.filters.urlextractortoheader

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.urlextractortoheader.config.UrlExtractorToHeaderConfig

import scala.collection.JavaConverters._
import scala.util.matching.Regex

@Named
class UrlExtractorToHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[UrlExtractorToHeaderConfig] with StrictLogging {

  import UrlExtractorToHeaderFilter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var extractions: Iterable[Extraction] = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("URL Extractor to Header filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info("Initializing URL Extractor to Header filter using config {}", configurationFile)
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[UrlExtractorToHeaderConfig])

    logger.trace("URL Extractor to Header filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

      extractions.foreach { extraction =>
        (extraction.urlRegex.findFirstMatchIn(httpRequest.getRequestURI), extraction.defaultValue) match {
          case (Some(regexMatch), _) =>
            (1 to regexMatch.groupCount)
              .map(regexMatch.group)
              .foreach(httpRequest.addHeader(extraction.headerName, _))
          case (None, Some(defaultValue)) => httpRequest.addHeader(extraction.headerName, defaultValue)
          case (None, None) => // don't add a header
        }
      }

      filterChain.doFilter(httpRequest, servletResponse)
    }
  }

  override def destroy(): Unit = {
    logger.trace("URL Extractor to Header filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("URL Extractor to Header filter destroyed.")
  }

  override def configurationUpdated(config: UrlExtractorToHeaderConfig): Unit = {
    extractions = config.getExtraction.asScala.map { extraction =>
      Extraction(extraction.getHeader, extraction.getUrlRegex.r, Option(extraction.getDefault))
    }
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object UrlExtractorToHeaderFilter {
  private final val DEFAULT_CONFIG = "url-extractor-to-header.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/url-extractor-to-header.xsd"

  case class Extraction(headerName: String, urlRegex: Regex, defaultValue: Option[String])

}
