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
package org.openrepose.filters.urinormalization

import java.net.URL
import java.util.Optional
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.normal.QueryStringNormalizer
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.{MetricNameUtility, MetricsService}
import org.openrepose.filters.urinormalization.config.{HttpMethod, UriNormalizationConfig}
import org.openrepose.filters.urinormalization.normalizer.{MediaTypeNormalizer, MultiInstanceWhiteListFactory}

import scala.collection.JavaConversions._
import scala.collection.mutable

@Named
class UriNormalizationFilter @Inject()(configurationService: ConfigurationService, optMetricsService: Optional[MetricsService])
  extends Filter with UpdateListener[UriNormalizationConfig] with StrictLogging {

  private final val DefaultConfig: String = "uri-normalization.cfg.xml"
  private final val NormalizationMetricPrefix = MetricRegistry.name(classOf[UriNormalizationFilter], "Normalization")

  private val metricRegistryOpt = Option(optMetricsService.orElse(null))

  private var initialized: Boolean = false
  private var configFilename: String = _
  private var mediaTypeNormalizer: MediaTypeNormalizer = _
  private var queryStringNormalizers: Iterable[QueryParameterNormalizer] = _

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

      mediaTypeNormalizer.normalizeContentMediaType(request)
      if (request.getParameterMap.nonEmpty) {
        queryStringNormalizers.find(_.normalize(request)) foreach { queryStringNormalizer =>
          metricRegistryOpt foreach {
            _.createSummingMeterFactory(NormalizationMetricPrefix)
              .createMeter(MetricRegistry.name(request.getMethod, MetricNameUtility.safeReportingName(queryStringNormalizer.getLastMatch.toString)))
              .mark()
          }
        }
      }

      filterChain.doFilter(request, servletResponse)
    }
  }

  override def isInitialized: Boolean = initialized

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configFilename, this)
  }

  override def init(filterConfig: FilterConfig): Unit = {
    configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfig)
    logger.info("Initializing filter using config " + configFilename)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/uri-normalization-configuration.xsd")
    configurationService.subscribeTo(filterConfig.getFilterName,
      configFilename,
      xsdURL,
      this,
      classOf[UriNormalizationConfig])
  }

  override def configurationUpdated(configurationObject: UriNormalizationConfig): Unit = {
    synchronized {
      val newNormalizers = mutable.LinkedHashMap.empty[String, QueryParameterNormalizer]

      Option(configurationObject.getUriFilters).foreach(_.getTarget foreach { target =>
        val alphabetize = target.isAlphabetize
        val whiteListFactory = new MultiInstanceWhiteListFactory(target.getWhitelist)
        val normalizerInstance = new QueryStringNormalizer(whiteListFactory, alphabetize)

        if (target.getHttpMethods.isEmpty) {
          target.getHttpMethods.add(HttpMethod.ALL)
        }

        target.getHttpMethods foreach { method =>
          val methodScopedNormalizer = newNormalizers.getOrElseUpdate(method.name, new QueryParameterNormalizer(method))
          methodScopedNormalizer.uriSelector.addPattern(target.getUriRegex, normalizerInstance)
        }
      })

      queryStringNormalizers = newNormalizers.values
      Option(configurationObject.getMediaVariants) match {
        case Some(mediaVariants) =>
          mediaTypeNormalizer = new MediaTypeNormalizer(mediaVariants.getMediaType)
        case None =>
          mediaTypeNormalizer = new MediaTypeNormalizer(Seq.empty)
      }

      initialized = true
    }
  }
}
