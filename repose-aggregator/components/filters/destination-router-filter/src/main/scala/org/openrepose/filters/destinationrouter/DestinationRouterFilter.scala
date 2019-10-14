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
package org.openrepose.filters.destinationrouter

import java.util
import java.util.Optional
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.commons.utils.servlet.http.RouteDestination
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.filters.routing.servlet.config.DestinationRouterConfiguration

@Named
class DestinationRouterFilter @Inject()(configurationService: ConfigurationService, optMetricsService: Optional[MetricsService])
  extends Filter with UpdateListener[DestinationRouterConfiguration] with StrictLogging {

  private final val DefaultConfigFileName = "destination-router.cfg.xml"
  private final val SchemaFilePath = "/META-INF/schema/config/destination-router-configuration.xsd"
  private final val RoutedResponseMetricPrefix = MetricRegistry.name(classOf[DestinationRouterFilter], "Routed Response")

  private var initialized: Boolean = false
  private var configurationFileName: String = _
  private var configuration: DestinationRouterConfiguration = _
  private val metricsService = Option(optMetricsService.orElse(null))

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Destination Router Filter initializing")
    configurationFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfigFileName)

    logger.info(s"Initializing Destination Router Filter using config $configurationFileName")
    val xsdUrl = getClass.getResource(SchemaFilePath)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFileName, xsdUrl, this, classOf[DestinationRouterConfiguration])

    logger.trace("Destination Router filter initialized")
  }

  override def destroy(): Unit = {
    logger.trace("Destination Router Filter destroying")
    configurationService.unsubscribeFrom(configurationFileName, this)

    logger.trace("Destination Router Filter destroyed")
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest = request.asInstanceOf[HttpServletRequest]
    val httpResponse = response.asInstanceOf[HttpServletResponse]

    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val target = configuration.getTarget

      if (Option(target.getId).forall(_.isEmpty)) {
        logger.warn("No destination configured for routing")
      } else {
        logger.info(s"Adding destination with id ${target.getId} and quality ${target.getQuality}")
        val targetDestination = new RouteDestination(target.getId, httpRequest.getRequestURI, target.getQuality)
        httpRequest.getAttribute(CommonRequestAttributes.DESTINATIONS) match {
          case null =>
            val newDestinations = new util.ArrayList[RouteDestination]()
            newDestinations.add(targetDestination)
            httpRequest.setAttribute(CommonRequestAttributes.DESTINATIONS, newDestinations)
          case destinations: java.util.List[RouteDestination] =>
            destinations.add(targetDestination)
          case _ =>
            logger.error("The destination could not be added -- the destinations attribute was of an unknown type")
        }

        metricsService foreach {
          _.createSummingMeterFactory(RoutedResponseMetricPrefix)
            .createMeter(target.getId)
            .mark()
        }
      }

      chain.doFilter(httpRequest, httpResponse)
    }
  }

  override def isInitialized: Boolean = initialized

  override def configurationUpdated(configurationObject: DestinationRouterConfiguration): Unit = {
    // Set the default quality since XJC won't
    val target = configurationObject.getTarget
    if (!target.isSetQuality) target.setQuality(0.5)

    configuration = configurationObject

    initialized = true
  }
}
