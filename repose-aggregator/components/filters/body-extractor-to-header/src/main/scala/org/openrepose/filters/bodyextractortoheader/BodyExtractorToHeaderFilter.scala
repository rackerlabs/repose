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

import com.jayway.jsonpath.{Configuration, JsonPath, Option => JsonOption}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodyextractortoheader.config.BodyExtractorToHeaderConfig

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

@Named
class BodyExtractorToHeaderFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[BodyExtractorToHeaderConfig] with LazyLogging {

  import BodyExtractorToHeaderFilter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var extractions: Iterable[Extraction] = _
  val jsonPathConfiguration = Configuration.defaultConfiguration()
  jsonPathConfiguration.addOptions(JsonOption.DEFAULT_PATH_LEAF_TO_NULL)

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

    extractions.foreach { extraction =>
      val extracted = Try(
        JsonPath.using(jsonPathConfiguration)
          .parse(mutableHttpRequest.getInputStream)
          .read[Any](extraction.jsonPath)
      )

      (extracted, extraction.defaultValue, extraction.nullValue) match {
        // JSONPath value was extracted AND is NOT Null
        case (Success(headerValue), _, _) if Option(headerValue).isDefined =>
          mutableHttpRequest.addHeader(extraction.headerName, headerValue.toString)
        // JSONPath value was extracted AND is Null AND NullValue is defined
        case (Success(headerValue), _, Some(nullValue)) =>
          mutableHttpRequest.addHeader(extraction.headerName, nullValue)
        // JSONPath value was NOT extracted AND DefaultValue is defined
        case (Failure(e), Some(defaultValue), _) =>
          mutableHttpRequest.addHeader(extraction.headerName, defaultValue)
        case (_, _, _) => // don't add a header
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
      Extraction(extraction.getHeader, extraction.getBodyJpath, Option(extraction.getDefault), Option(extraction.getNullValue))
    }
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object BodyExtractorToHeaderFilter {
  private final val DEFAULT_CONFIG = "body-extractor-to-header.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/body-extractor-to-header.xsd"

  case class Extraction(headerName: String, jsonPath: String, defaultValue: Option[String], nullValue: Option[String])

}