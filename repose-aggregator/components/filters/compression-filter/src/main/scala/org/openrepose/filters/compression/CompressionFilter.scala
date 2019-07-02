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

package org.openrepose.filters.compression

import java.io.{EOFException, IOException}
import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.external.pjlcompression.CompressingFilter
import org.openrepose.filters.compression.CompressionFilter.CompressionParameters.CompressionParameters
import org.openrepose.filters.compression.config.ContentCompressionConfig

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Named
class CompressionFilter @Inject()(configurationService: ConfigurationService, compressingFilterFactory: CompressingFilterFactory)
  extends Filter with UpdateListener[ContentCompressionConfig] with StrictLogging {

  import CompressionFilter._
  import CompressionParameters._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false

  // keep it around so we can initialize the filter that's doing the actual work
  private var filterConfig: FilterConfig = _

  // filter that's doing the actual work
  private var actualFilter: CompressingFilter = _


  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Compression filter initializing...")
    this.filterConfig = filterConfig
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing Compression filter using config $configurationFile")
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[ContentCompressionConfig])

    logger.trace("Compression filter initialized.")
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val httpResponse = response.asInstanceOf[HttpServletResponse]

      val filterResult: ActualFilterResult = Try(actualFilter.doFilter(request, httpResponse, filterChain)) match {
        case Success(_) => Pass
        case Failure(ioe: IOException) if "Not in GZIP format".equalsIgnoreCase(ioe.getMessage) => BadRequest(ioe)
        case Failure(ioe: IOException) if classOf[EOFException] == ioe.getClass => BadRequest(ioe)
        case Failure(ioe: IOException) => InternalError(ioe)
        case Failure(e: Exception) => InternalError(e)
      }

      filterResult match {
        case Pass => // do nothing
        case BadRequest(e: Exception) =>
          logger.warn("Unable to decompress message. Bad request body or content-encoding")
          logger.debug("Exception: ", e)
          httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        case InternalError(e: Exception) =>
          logger.error("Error with the CompressingFilter", e)
          httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
  }

  override def configurationUpdated(config: ContentCompressionConfig): Unit = {
    val compConfig = config.getCompression

    val actualFilterConfig = new CompressingFilterConfig(filterConfig)
    actualFilterConfig.setInitParameter(StatsEnabled, compConfig.isStatsEnabled.toString)
    actualFilterConfig.setInitParameter(JavaUtilLogger, logger.underlying.getName)
    actualFilterConfig.setInitParameter(Debug, compConfig.isDebug.toString)
    actualFilterConfig.setInitParameter(CompressionThreshold, compConfig.getCompressionThreshold.toString)

    (compConfig.getIncludeContentTypes.isEmpty, compConfig.getExcludeContentTypes.isEmpty) match {
      case (false, _) => actualFilterConfig.setInitParameter(IncludeContentTypes, compConfig.getIncludeContentTypes.asScala.mkString(","))
      case (true, false) => actualFilterConfig.setInitParameter(ExcludeContentTypes, compConfig.getExcludeContentTypes.asScala.mkString(","))
      case (true, true) => // do nothing
    }

    (compConfig.getIncludeUserAgentPatterns.isEmpty, compConfig.getExcludeUserAgentPatterns.isEmpty) match {
      case (false, _) => actualFilterConfig.setInitParameter(IncludeUserAgentPatterns, compConfig.getIncludeUserAgentPatterns.asScala.mkString(","))
      case (true, false) => actualFilterConfig.setInitParameter(ExcludeUserAgentPatterns, compConfig.getExcludeUserAgentPatterns.asScala.mkString(","))
      case (true, true) => // do nothing
    }

    actualFilter = compressingFilterFactory.newCompressingFilter(actualFilterConfig)
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object CompressionFilter {
  private final val DEFAULT_CONFIG = "compression.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/content-compression-configuration.xsd"

  implicit def autoCompressionParameterToString(param: CompressionParameters): String = param.toString

  sealed trait ActualFilterResult
  object Pass extends ActualFilterResult
  case class BadRequest(e: Exception) extends ActualFilterResult
  case class InternalError(e: Exception) extends ActualFilterResult

  object CompressionParameters extends Enumeration {
    type CompressionParameters = Value
    val Debug = Value("debug")
    val CompressionThreshold = Value("compressionThreshold")
    val StatsEnabled = Value("statsEnabled")
    val IncludeContentTypes = Value("includeContentTypes")
    val ExcludeContentTypes = Value("excludeContentTypes")
    val IncludeUserAgentPatterns = Value("includeUserAgentPatterns")
    val ExcludeUserAgentPatterns = Value("excludeUserAgentPatterns")
    val JavaUtilLogger = Value("javaUtilLogger")
  }
}
