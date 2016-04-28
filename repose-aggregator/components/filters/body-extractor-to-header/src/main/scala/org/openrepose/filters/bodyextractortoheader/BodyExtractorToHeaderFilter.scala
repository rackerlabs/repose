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
package org.openrepose.filters.bodyextractortoheader

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodyextractortoheader.config.BodyExtractorToHeaderConfig

import scala.collection.JavaConverters._

@Named
class BodyExtractorToHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[BodyExtractorToHeaderConfig] with LazyLogging {

  import BodyExtractorToHeaderFilter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var extractions: Iterable[Extraction] = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Body Extractor to Header filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info("Initializing Body Extractor to Header filter using config {}", configurationFile)
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[BodyExtractorToHeaderConfig])

    logger.trace("Body Extractor to Header filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])

    def doExtraction(extraction: Extraction): Option[String] = {
      // TODO: Do the JPath extraction.
      throw new UnsupportedOperationException("doExtraction is not yet implemented")
    }

    extractions.foreach { extraction =>
      (doExtraction(extraction), extraction.defaultValue) match {
        case (Some(headerValue), _) => mutableHttpRequest.addHeader(extraction.headerName, headerValue)
        case (None, Some(defaultValue)) => mutableHttpRequest.addHeader(extraction.headerName, defaultValue)
        case (None, None) => // don't add a header
      }
    }

    filterChain.doFilter(mutableHttpRequest, servletResponse)
  }

  override def destroy(): Unit = {
    logger.trace("Body Extractor to Header filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Body Extractor to Header filter destroyed.")
  }

  override def configurationUpdated(config: BodyExtractorToHeaderConfig): Unit = {
    extractions = config.getExtraction.asScala.map { extraction =>
      Extraction(extraction.getHeader, extraction.getBodyJpath, Option(extraction.getDefault))
    }
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object BodyExtractorToHeaderFilter {
  private final val DEFAULT_CONFIG = "body-extractor-to-header.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/body-extractor-to-header.xsd"

  case class Extraction(headerName: String, jsonPath: String, defaultValue: Option[String])

}