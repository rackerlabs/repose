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
package org.openrepose.filters.ipuser

import java.net.{URL, UnknownHostException}
import java.util.concurrent.atomic.AtomicReference

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.typesafe.scalalogging.slf4j.StrictLogging
import edazdarevic.commons.net.CIDRUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.ipuser.config.IpUserConfig

@Named
class IpUserFilter @Inject()(configurationService: ConfigurationService) extends Filter
  with StrictLogging
  with UpdateListener[IpUserConfig] {

  private final val DEFAULT_CONFIG = "ip-user.cfg.xml"

  private val cidrList: AtomicReference[List[LabeledCIDR]] = new AtomicReference[List[LabeledCIDR]]()

  private var initialized = false
  private var configName: String = _
  private var groupHeaderName: String = _
  private var groupHeaderQuality: Double = _
  private var userHeaderName: String = _
  private var userHeaderQuality: Double = _

  override def init(filterConfig: FilterConfig): Unit = {
    configName = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing using config $configName")
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/ip-user.xsd")

    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configName,
      xsdURL,
      this,
      classOf[IpUserConfig]
    )
  }

  override def destroy(): Unit = configurationService.unsubscribeFrom(configName, this.asInstanceOf[UpdateListener[_]])

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      logger.trace("IP User filter handling request...")
      val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])

      val clientIpAddress = request.getSplittableHeaderScala(CommonHttpHeader.X_FORWARDED_FOR)
        .headOption.getOrElse(servletRequest.getRemoteAddr)

      try {
        getClassificationLabel(clientIpAddress).foreach { label =>
          request.addHeader(groupHeaderName, label, groupHeaderQuality)
        }

        //Always set the user header name to the current IP address
        request.addHeader(userHeaderName, clientIpAddress, userHeaderQuality)

        logger.trace("IP User filter passing request...")
        filterChain.doFilter(request, servletResponse)
      } catch {
        case uhe: UnknownHostException =>
          logger.debug(s"Bad X-Forwarded-For header: $clientIpAddress", uhe)
          servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed X-Forwarded-For header")
      }
      logger.trace("IP User filter returning response...")
    }
  }

  def getClassificationLabel(ipAddress: String): Option[String] = {
    cidrList.get().find { labeledCidr =>
      labeledCidr.cidr.isInRange(ipAddress)
    } map { foundLabeledCidr =>
      foundLabeledCidr.label
    }
  }

  override def configurationUpdated(classificationConfig: IpUserConfig): Unit = {
    //Update all the CIDRs
    //Create a new list to replace the old one

    import scala.collection.JavaConversions._
    val groups = classificationConfig.getGroup.toList

    /**
      * This guy builds a List[List[LabeledCIDR]] I flat map it to remove that extra list, so it's just a
      * List[LabeledCIDR]. I suppose I could separately transform the classification lines into lists, and then combine
      * them, but this does the same thing
      */
    val replacementCidrList: List[LabeledCIDR] = groups.flatMap { group =>
      val label = group.getName
      group.getCidrIp.map { cidr =>
        LabeledCIDR(label, new CIDRUtils(cidr))
      }
    }

    //Replace our object! Atomically
    cidrList.set(replacementCidrList)

    //Blergh, no useful defaults in XSD when I add complex types :(
    groupHeaderName = Option(classificationConfig.getGroupHeader).map { header =>
      header.getName
    } getOrElse "x-pp-groups"

    groupHeaderQuality = Option(classificationConfig.getGroupHeader).map { header =>
      header.getQuality
    } getOrElse 0.4D

    userHeaderName = Option(classificationConfig.getUserHeader).map { header =>
      header.getName
    } getOrElse "x-pp-user"

    userHeaderQuality = Option(classificationConfig.getUserHeader).map { header =>
      header.getQuality
    } getOrElse 0.4D

    initialized = true
  }

  override def isInitialized: Boolean = initialized

  case class LabeledCIDR(label: String, cidr: CIDRUtils)

}
