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
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filters.HeaderNormalization
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.{MeterByCategorySum, MetricsService}
import org.openrepose.filters.headernormalization.HeaderNormalizationFilter._
import org.openrepose.filters.headernormalization.config.{HeaderNormalizationConfig, HttpMethod}

import scala.collection.JavaConverters._

@Named
class HeaderNormalizationFilter @Inject()(configurationService: ConfigurationService, metricsService: MetricsService)
  extends Filter with UpdateListener[HeaderNormalizationConfig] with LazyLogging {

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var configRequest: Seq[Target] = _
  private var configResponse: Seq[Target] = _
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
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
      return
    }

    val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

    configRequest find { target =>
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

      metricsMeter.mark(s"${target.url}_${wrappedRequest.getMethod}_request")
    }

    val httpServletResponse =
      if (configResponse.isEmpty) servletResponse.asInstanceOf[HttpServletResponse]
      else new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse], MUTABLE, PASSTHROUGH)

    filterChain.doFilter(wrappedRequest, httpServletResponse)

    configResponse find { target =>
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

      metricsMeter.mark(s"${target.url}_${wrappedRequest.getMethod}_response")
    }
  }

  override def destroy(): Unit = {
    logger.trace("Header Normalization filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Header Normalization filter destroyed.")
  }

  override def configurationUpdated(config: HeaderNormalizationConfig): Unit = {
    val oldTargets = Option(config.getHeaderFilters) map { hdrFilter => hdrFilter.getTarget }
    this.configRequest = oldTargets.getOrElse(config.getTarget).asScala map { target =>
      val targetRequest = Option(target.getRequest)
      val access = if (target.getBlacklist.isEmpty &&
        (targetRequest.isEmpty || Option(targetRequest.get.getBlacklist).isEmpty)) WhiteList else BlackList
      val headers = (access match {
        case WhiteList =>
          val oldWhiteList = target.getWhitelist
          if (oldWhiteList.isEmpty) targetRequest.get.getWhitelist else oldWhiteList.get(0)
        case BlackList =>
          val oldBlackList = target.getBlacklist
          if (oldBlackList.isEmpty) targetRequest.get.getBlacklist else oldBlackList.get(0)
      }).getHeader.asScala.map(_.getId).toSet

      Target(
        Option(target.getUriRegex).getOrElse(".*").r.pattern, // if not configured, default is ".*"
        target.getHttpMethods.asScala.map(_.toString).padTo(1, AllHttpMethods).toSet, // default is "ALL"
        RequestTarget,
        access,
        headers)
    }

    this.configResponse = Seq.empty[Target]

    initialized = true
  }

  override def isInitialized: Boolean = initialized

}

object HeaderNormalizationFilter {
  private final val DEFAULT_CONFIG = "header-normalization.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/header-normalization-configuration.xsd"

  val AllHttpMethods: String = HttpMethod.ALL.toString

  sealed trait StyleType
  object NewStyle extends StyleType
  object OldStyle extends StyleType

  sealed trait TargetType
  object RequestTarget extends TargetType
  object ResponseTarget extends TargetType

  sealed trait AccessList
  object WhiteList extends AccessList
  object BlackList extends AccessList

  case class Target(url: Pattern, methods: Set[String], reqRes: TargetType, access: AccessList, headers: Set[String])
}
