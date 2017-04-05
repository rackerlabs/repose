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
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.ResponseMode.{MUTABLE, PASSTHROUGH}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.commons.utils.string.RegexString
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.filters.headernormalization.HeaderNormalizationFilter._
import org.openrepose.filters.headernormalization.config.{HeaderNormalizationConfig, HttpHeaderList, HttpMethod, Target => ConfigTarget}

import scala.collection.JavaConverters._

@Named
class HeaderNormalizationFilter @Inject()(configurationService: ConfigurationService, metricsService: MetricsService)
  extends Filter with UpdateListener[HeaderNormalizationConfig] with LazyLogging {

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized = false
  private var configRequest: Seq[Target] = _
  private var configResponse: Seq[Target] = _
  private val metricsServiceOption = Option(metricsService)

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Header Normalization filter initializing...")
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info(s"Initializing Header Normalization filter using config $configurationFile")
    val xsdUrl: URL = getClass.getResource(SCHEMA_FILE_NAME)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFile, xsdUrl, this, classOf[HeaderNormalizationConfig])

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
      target.url =~ wrappedRequest.getRequestURI &&
        (target.methods.contains(wrappedRequest.getMethod) || target.methods.contains(AllHttpMethods))
    } foreach { target =>
      // figure out which headers to remove, and remove them
      (target.access match {
        case WhiteList => wrappedRequest.getHeaderNamesScala.diff(target.headers)
        case BlackList => target.headers
      }).foreach(wrappedRequest.removeHeader)

      metricsServiceOption.foreach(metricsService =>
        metricsService.getRegistry.meter(metricsService.name(
          "org.openrepose.core.filters.HeaderNormalization",
          "header-normalization",
          "Normalization",
          s"${target.url.pattern.toString}_${wrappedRequest.getMethod}_request"))
          .mark())
    }

    val wrappedResponse = if (configResponse.isEmpty) None else Option(
      new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse], MUTABLE, PASSTHROUGH))

    filterChain.doFilter(wrappedRequest, wrappedResponse.getOrElse(servletResponse.asInstanceOf[HttpServletResponse]))

    var responseHeaders: Option[Set[String]] = None

    def getResponseHeaders: Set[String] = {
      if (responseHeaders.isEmpty) {
        responseHeaders = Option(wrappedResponse.get.getHeaderNames.asScala.map(_.toLowerCase).toSet)
      }
      responseHeaders.get
    }

    configResponse find { target =>
      // find the first "target" config element that matches this request (if any)
      target.url =~ wrappedRequest.getRequestURI &&
        (target.methods.contains(wrappedRequest.getMethod) || target.methods.contains(AllHttpMethods))
    } foreach { target =>
      // figure out which headers to remove, and remove them
      (target.access match {
        case WhiteList => getResponseHeaders.diff(target.headers)
        case BlackList => target.headers
      }).foreach(wrappedResponse.get.removeHeader)

      metricsServiceOption.foreach(metricsService =>
        metricsService.getRegistry.meter(
          metricsService.name(
            "org.openrepose.core.filters.HeaderNormalization",
            "header-normalization",
            "Normalization",
            s"${target.url.pattern.toString}_${wrappedRequest.getMethod}_response"))
          .mark())
    }

    if (wrappedResponse.isDefined) wrappedResponse.get.commitToResponse()
  }

  override def destroy(): Unit = {
    logger.trace("Header Normalization filter destroying...")
    configurationService.unsubscribeFrom(configurationFile, this.asInstanceOf[UpdateListener[_]])
    logger.trace("Header Normalization filter destroyed.")
  }

  override def configurationUpdated(config: HeaderNormalizationConfig): Unit = {
    def getTarget(target: ConfigTarget, accessList: AccessList, headers: HttpHeaderList): Option[Target] = {
      Option(Target(
        new RegexString(Option(target.getUriRegex).getOrElse(".*")), // if not configured, default is ".*"
        target.getHttpMethods.asScala.map(_.toString).padTo(1, AllHttpMethods).toSet, // default is "ALL"
        accessList,
        headers.getHeader.asScala.map(_.getId.toLowerCase).toSet))
    }

    val targets = (Option(config.getHeaderFilters) map { hdrFilter =>
      logger.warn("Your Header Normalization configuration will not be compatible with v10.0.0.0.")
      logger.warn("Please refer to the documentation to update your Header Normalization configuration accordingly.")
      hdrFilter.getTarget
    }).getOrElse(config.getTarget).asScala

    this.configRequest = targets flatMap { target =>
      if (Option(target.getRequest).isDefined) {
        val targetRequest = target.getRequest
        if (Option(targetRequest.getBlacklist).isDefined) {
          getTarget(target, BlackList, targetRequest.getBlacklist)
        } else if (Option(targetRequest.getWhitelist).isDefined) {
          getTarget(target, WhiteList, targetRequest.getWhitelist)
        } else {
          None
        }
      } else {
        logger.warn("Your Header Normalization configuration will not be compatible with v10.0.0.0.")
        logger.warn("Please refer to the documentation to update your Header Normalization configuration accordingly.")
        if (!target.getBlacklist.isEmpty) {
          getTarget(target, BlackList, target.getBlacklist.get(0))
        } else if (!target.getWhitelist.isEmpty) {
          getTarget(target, WhiteList, target.getWhitelist.get(0))
        } else {
          None
        }
      }
    }

    this.configResponse = targets flatMap { target =>
      if (Option(target.getResponse).isDefined) {
        val targetResponse = target.getResponse
        if (Option(targetResponse.getBlacklist).isDefined) {
          getTarget(target, BlackList, targetResponse.getBlacklist)
        } else if (Option(targetResponse.getWhitelist).isDefined) {
          getTarget(target, WhiteList, targetResponse.getWhitelist)
        } else {
          None
        }
      } else {
        None
      }
    }

    initialized = true
  }

  override def isInitialized: Boolean = initialized

}

object HeaderNormalizationFilter {
  private final val DEFAULT_CONFIG = "header-normalization.cfg.xml"
  private final val SCHEMA_FILE_NAME = "/META-INF/schema/config/header-normalization-configuration.xsd"

  val AllHttpMethods: String = HttpMethod.ALL.toString

  sealed trait AccessList

  object WhiteList extends AccessList

  object BlackList extends AccessList

  case class Target(url: RegexString, methods: Set[String], access: AccessList, headers: Set[String])

}
