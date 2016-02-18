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

package org.openrepose.filters.headernormalization

import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filters.HeaderNormalization
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.{MeterByCategorySum, MetricsService}
import org.openrepose.filters.headernormalization.config.{HttpMethod, HeaderNormalizationConfig}

import scala.collection.JavaConverters._

@Named
class HeaderNormalizationFilter @Inject()(configurationService: ConfigurationService, metricsService: MetricsService)
  extends Filter with UpdateListener[HeaderNormalizationConfig] with LazyLogging {

  import HeaderNormalizationFilter._

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var config: Seq[Target] = _
  private var metricsMeter: MeterByCategorySum = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Header Normalization filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing Header Normalization filter using config $configurationFile")
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[HeaderNormalizationConfig])

    metricsMeter = metricsService.newMeterByCategorySum(classOf[HeaderNormalization], "header-normalization", "Normalization", TimeUnit.SECONDS)

    logger.trace("Header Normalization filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.warn("Header Normalization filter has not yet initialized.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
      return
    }

    val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

    config find { target =>
      // find the first "target" config element that matches this request (if any)
      target.url.matcher(wrappedRequest.getRequestURI).matches &&
        (target.methods.contains(wrappedRequest.getMethod) || target.methods.contains(AllHttpMethods))
    } foreach { target =>
      // figure out which headers to remove, and remove them
      (target.access match {
        case WhiteList => wrappedRequest.getHeaderNamesScala.diff(target.headers)
          // written like this to maintain the case-insensitive string comparisons - do not swap
        case BlackList => target.headers.intersect(wrappedRequest.getHeaderNamesScala)
      }).foreach(wrappedRequest.removeHeader)

      metricsMeter.mark(s"${target.url}_${wrappedRequest.getMethod}")
    }

    filterChain.doFilter(wrappedRequest, servletResponse)
  }

  override def destroy(): Unit = {
    logger.trace("Header Normalization filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Header Normalization filter destroyed.")
  }

  override def configurationUpdated(config: HeaderNormalizationConfig): Unit = {
    this.config = config.getHeaderFilters.getTarget.asScala map { target =>
      val access = if (target.getBlacklist.isEmpty) WhiteList else BlackList
      val headers = (access match {
        case WhiteList => target.getWhitelist
        case BlackList => target.getBlacklist
      }).get(0).getHeader.asScala.map(_.getId).toSet

      Target(
        Option(target.getUriRegex).getOrElse(".*").r.pattern,       // if not configured, default is ".*"
        target.getHttpMethods.asScala.map(_.toString).padTo(1, AllHttpMethods).toSet,  // default is "ALL"
        access,
        headers)
    }

    initialized = true
  }

  override def isInitialized: Boolean = initialized

}

object HeaderNormalizationFilter {
  private final val DEFAULT_CONFIG = "header-normalization.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/header-normalization-configuration.xsd"

  val AllHttpMethods = HttpMethod.ALL.toString

  sealed trait AccessList
  object WhiteList extends AccessList
  object BlackList extends AccessList

  case class Target(url: Pattern, methods: Set[String], access: AccessList, headers: Set[String])
}