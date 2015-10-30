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
package org.openrepose.filters.ipclassification

import java.net.URL
import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import edazdarevic.commons.net.CIDRUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.ipclassification.config.IpClassificationConfig

@Named
class IPClassificationFilter @Inject()(configurationService: ConfigurationService) extends Filter
with LazyLogging
with UpdateListener[IpClassificationConfig] {

  private final val DEFAULT_CONFIG = "ip-classification.cfg.xml"
  private var initialized = false
  private var config: String = _

  type CIDRTuple = (String, CIDRUtils)
  private val cidrList = new ConcurrentLinkedQueue[CIDRTuple]()
  private var groupHeaderName: String = _
  private var groupHeaderQuality: Double = _
  private var userHeaderName: String = _
  private var userHeaderQuality: Double = _

  override def init(filterConfig: FilterConfig): Unit = {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing using config $config")
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/ip-classification.xsd")

    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      this,
      classOf[IpClassificationConfig]
    )
  }

  override def destroy(): Unit = configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("IP Classification filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(500)
    } else {
      logger.trace("IP Classification filter handling request...")
      val request = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])

      getClassificationLabel(servletRequest.getRemoteAddr).foreach { label =>
        request.addHeader(groupHeaderName, s"$label;q=$groupHeaderQuality")
      }

      //Always set the user header name to the current IP address
      request.addHeader(userHeaderName, s"${servletRequest.getRemoteAddr};q=$userHeaderQuality")

      logger.trace("IP Classification filter passing request...")
      filterChain.doFilter(request, servletResponse)

      logger.trace("IP Classification filter returning response...")
    }
  }

  def getClassificationLabel(ipAddress: String): Option[String] = {
    import scala.collection.JavaConversions._
    cidrList.toList.find { case (_, cidrUtil) =>
      cidrUtil.isInRange(ipAddress)
    } map { case (label, _) =>
      label
    }
  }

  override def configurationUpdated(config: IpClassificationConfig): Unit = {
    //Update all the CIDRs
    //Create a new list to replace the old one
    val items = new util.ArrayList[CIDRTuple]()

    import scala.collection.JavaConversions._
    val classifications = config.getClassifications.getClassification.toList
    classifications.foreach { classification =>
      val label = classification.getLabel
      def splitCIDR(javaCIDR: String): List[String] = {

        Option(javaCIDR).map { cidr =>
          cidr.split(" ").toList
        } getOrElse {
          List.empty[String]
        }
      }

      splitCIDR(classification.getIpv4Cidr).map { cidr =>
        items.add((label, new CIDRUtils(cidr)))
      }
      splitCIDR(classification.getIpv6Cidr).map { cidr =>
        items.add((label, new CIDRUtils(cidr)))
      }
    }

    //We have loaded all the config, clear it and add all the new ones
    cidrList.clear()
    cidrList.addAll(items)

    //Blergh, no useful defaults in XSD when I add complex types :(
    groupHeaderName = Option(config.getGroupHeaderName).map { headerName =>
      headerName.getValue
    } getOrElse "x-pp-groups"

    groupHeaderQuality = Option(config.getGroupHeaderName).map { headerName =>
      headerName.getQuality
    } getOrElse 0.4D

    userHeaderName = Option(config.getUserHeaderName).map { headerName =>
      headerName.getValue
    } getOrElse "x-pp-user"

    userHeaderQuality = Option(config.getUserHeaderName).map { headerName =>
      headerName.getQuality
    } getOrElse 0.4D

    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
