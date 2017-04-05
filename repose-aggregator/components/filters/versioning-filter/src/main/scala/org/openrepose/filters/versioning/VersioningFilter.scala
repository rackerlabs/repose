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
package org.openrepose.filters.versioning

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.bind.JAXBElement

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.HttpHeaders
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonRequestAttributes
import org.openrepose.commons.utils.http.media.MediaType
import org.openrepose.commons.utils.io.RawInputStreamReader
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, RouteDestination}
import org.openrepose.core.filter.{FilterConfigHelper, SystemModelInterrogator}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.{HealthCheckService, Severity}
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.{Destination, SystemModel}
import org.openrepose.filters.versioning.config.{ServiceVersionMapping, ServiceVersionMappingList}
import org.openrepose.filters.versioning.domain.{ConfigurationData, VersionedHostNotFoundException, VersionedRequest}
import org.openrepose.filters.versioning.schema.ObjectFactory
import org.openrepose.filters.versioning.util.{ContentTransformer, RequestMediaRangeInterrogator, VersionChoiceFactory}
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConversions._

@Named
class VersioningFilter @Inject()(@Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                 @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                 configurationService: ConfigurationService,
                                 healthCheckService: HealthCheckService,
                                 metricsService: MetricsService)
  extends Filter with LazyLogging {

  private final val SystemModelConfigFileName = "system-model.cfg.xml"
  private final val DefaultVersioningConfigFileName = "versioning.cfg.xml"
  private final val VersioningSchemaFilePath = "/META-INF/schema/config/versioning-configuration.xsd"
  private final val SystemModelConfigIssue = "SystemModelConfigIssue"
  private final val VersioningDefaultQuality = 0.5

  private val versioningObjectFactory = new ObjectFactory()
  private val healthCheckServiceProxy = healthCheckService.register
  private val metricsServiceOption = Option(metricsService)

  private var configurationFileName: String = _
  private var serviceVersionMappings: Map[String, ServiceVersionMapping] = _
  private var destinations: Map[String, Destination] = _
  private var contentTransformer: ContentTransformer = _

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("Versioning Filter initializing")
    configurationFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultVersioningConfigFileName)

    logger.info(s"Initializing Versioning Filter using config $configurationFileName")
    val xsdUrl = getClass.getResource(VersioningSchemaFilePath)
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFileName,
      xsdUrl,
      VersioningConfigurationListener,
      classOf[ServiceVersionMappingList])
    configurationService.subscribeTo(
      SystemModelConfigFileName,
      SystemModelConfigurationListener,
      classOf[SystemModel])

    logger.trace("Versioning filter initialized")
  }

  override def destroy(): Unit = {
    logger.trace("Versioning Filter destroying")
    configurationService.unsubscribeFrom(configurationFileName, VersioningConfigurationListener)
    configurationService.unsubscribeFrom(SystemModelConfigFileName, SystemModelConfigurationListener)

    logger.trace("Versioning Filter destroyed")
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    lazy val wrappedRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    val httpResponse = response.asInstanceOf[HttpServletResponse]

    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val configurationData = new ConfigurationData(destinations, serviceVersionMappings)

      try {
        def markMeter(name: String): Unit = {
          metricsServiceOption.foreach(metricService =>
            metricService.getRegistry.meter(metricService.name(
              "org.openrepose.core.filters.Versioning",
              "versioning", // todo: replace "versioning" with filter-id or name-number in sys-model
              "VersionedRequest",
              name))
              .mark())
        }

        Option(configurationData.getOriginServiceForRequest(wrappedRequest)) match {
          case Some(targetOriginService) =>
            val versionedRequest = new VersionedRequest(wrappedRequest, targetOriginService.getMapping)

            // Is this a request to a version root we are aware of for describing it? (e.g. http://api.service.com/v1.0/)
            if (versionedRequest.isRequestForRoot || versionedRequest.requestMatchesVersionMapping) {
              val versionElement = versioningObjectFactory.createVersion(new VersionChoiceFactory(targetOriginService.getMapping).create)

              transformResponse(httpResponse, versionElement, getPreferredMediaRange(versionedRequest.getRequest))
              httpResponse.setStatus(HttpServletResponse.SC_OK)
              httpResponse.addHeader("Content-Type", getPreferredMediaRange(wrappedRequest).getMimeType.getName)

              markMeter(targetOriginService.getMapping.getId)
            } else {
              val targetDestination = new RouteDestination(
                targetOriginService.getOriginServiceHost.getId,
                versionedRequest.asInternalURI,
                VersioningDefaultQuality)
              targetDestination.setContextRemoved(versionedRequest.getMapping.getId)

              Option(versionedRequest.getRequest.getAttribute(CommonRequestAttributes.DESTINATIONS)) match {
                case None =>
                  val newDestinations = new java.util.ArrayList[RouteDestination]()
                  newDestinations.add(targetDestination)
                  versionedRequest.getRequest.setAttribute(CommonRequestAttributes.DESTINATIONS, newDestinations)
                case Some(destinations: java.util.List[RouteDestination]) =>
                  destinations.add(targetDestination)
                case Some(_) =>
                  logger.error("The destination could not be added -- the destinations attribute was of an unknown type")
              }

              markMeter(targetOriginService.getMapping.getId)

              chain.doFilter(wrappedRequest, httpResponse)
            }
          case None =>
            // Is this a request to the service root to describe the available versions? (e.g. http://api.service.com/)
            if (configurationData.isRequestForVersions(wrappedRequest)) {
              val versions = versioningObjectFactory.createVersions(configurationData.versionChoicesAsList(wrappedRequest))

              transformResponse(httpResponse, versions, getPreferredMediaRange(wrappedRequest))
              httpResponse.setStatus(HttpServletResponse.SC_OK)
            } else {
              // This is not a version we recognize - tell the client what's up
              val versionChoiceList = configurationData.versionChoicesAsList(wrappedRequest)
              val versionChoiceListElement = versioningObjectFactory.createChoices(versionChoiceList)

              transformResponse(httpResponse, versionChoiceListElement, getPreferredMediaRange(wrappedRequest))
              httpResponse.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES)
            }

            httpResponse.addHeader("Content-Type", getPreferredMediaRange(wrappedRequest).getMimeType.getName)

            markMeter("Unversioned")
        }
      } catch {
        case vhnfe: VersionedHostNotFoundException =>
          httpResponse.setStatus(HttpServletResponse.SC_BAD_GATEWAY)
          logger.warn("Configured versioned service mapping refers to a bad pp-dest-id. Reason: " + vhnfe.getMessage, vhnfe)
      }
    }
  }

  private def isInitialized: Boolean =
    SystemModelConfigurationListener.isInitialized && VersioningConfigurationListener.isInitialized

  private def getPreferredMediaRange(request: HttpServletRequestWrapper): MediaType = {
    RequestMediaRangeInterrogator.interrogate(
      request.getRequestURI,
      request.getPreferredSplittableHeaders(HttpHeaders.ACCEPT)).get(0)
  }

  private def transformResponse(response: HttpServletResponse, elementToMarshal: JAXBElement[_], preferredMediaType: MediaType): Unit = {
    val baos = new ByteArrayOutputStream()

    contentTransformer.transform(elementToMarshal, preferredMediaType, baos)

    RawInputStreamReader.instance.copyTo(new ByteArrayInputStream(baos.toByteArray), response.getOutputStream)
  }

  private object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      val interrogator = new SystemModelInterrogator(clusterId, nodeId)
      val localCluster = interrogator.getLocalCluster(configurationObject)
      val localNode = interrogator.getLocalNode(configurationObject)

      if (localCluster.isPresent && localNode.isPresent) {
        destinations = (localCluster.get().getDestinations.getEndpoint ++ localCluster.get.getDestinations.getTarget)
          .map(destination => destination.getId -> destination)
          .toMap

        healthCheckServiceProxy.resolveIssue(SystemModelConfigIssue)
        initialized = true
      } else {
        logger.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml")
        healthCheckServiceProxy.reportIssue(
          SystemModelConfigIssue,
          "Unable to identify the local host in the system model - please check your system-model.cfg.xml",
          Severity.BROKEN)
      }
    }

    override def isInitialized: Boolean = initialized
  }

  private object VersioningConfigurationListener extends UpdateListener[ServiceVersionMappingList] {
    private var initialized = false

    override def configurationUpdated(configurationObject: ServiceVersionMappingList): Unit = {
      serviceVersionMappings = configurationObject.getVersionMapping.map(svm => svm.getId -> svm).toMap
      contentTransformer = new ContentTransformer(configurationObject.getJsonFormat)
      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

}
