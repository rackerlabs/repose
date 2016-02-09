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
package org.openrepose.filters.openstackidentityv3

import java.net.URL
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.filter.FilterAction
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config
import org.openrepose.filters.openstackidentityv3.utilities.OpenStackIdentityV3API

@Named
class OpenStackIdentityV3Filter @Inject()(configurationService: ConfigurationService,
                                          datastoreService: DatastoreService,
                                          httpClientService: HttpClientService,
                                          akkaServiceClientFactory: AkkaServiceClientFactory)
  extends Filter with UpdateListener[OpenstackIdentityV3Config] with LazyLogging {

  private final val DEFAULT_CONFIG = "openstack-identity-v3.cfg.xml"

  private var initialized = false
  private var configFilename: String = _
  private var akkaServiceClient: AkkaServiceClient = _
  private var openStackIdentityV3Handler: OpenStackIdentityV3Handler = _

  override def init(filterConfig: FilterConfig) {
    configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configFilename)
    val xsdURL: URL = getClass.getResource("/META-INF/config/schema/openstack-identity-v3.xsd")
    configurationService.subscribeTo(filterConfig.getFilterName,
      configFilename,
      xsdURL,
      this,
      classOf[OpenstackIdentityV3Config])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    if (!isInitialized) {
      logger.error("OpenStack Identity v3 filter has not yet initialized...")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val requestWrapper = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val response = servletResponse.asInstanceOf[HttpServletResponse]

      val filterAction = openStackIdentityV3Handler.handleRequest(requestWrapper, response)
      filterAction match {
        case FilterAction.RETURN => // no action to take
        case FilterAction.PASS =>
          filterChain.doFilter(requestWrapper, response)
        case FilterAction.PROCESS_RESPONSE =>
          filterChain.doFilter(requestWrapper, response)
          openStackIdentityV3Handler.handleResponse(response)
        case FilterAction.NOT_SET =>
          logger.error("Unexpected internal filter state")
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }

  def isInitialized = initialized

  override def destroy() {
    Option(akkaServiceClient).foreach(_.destroy())
    configurationService.unsubscribeFrom(configFilename, this)
  }

  def configurationUpdated(config: OpenstackIdentityV3Config) {
    val akkaServiceClientOld = Option(akkaServiceClient)
    akkaServiceClient = akkaServiceClientFactory.newAkkaServiceClient(config.getConnectionPoolId)
    akkaServiceClientOld.foreach(_.destroy())

    val identityAPI = new OpenStackIdentityV3API(config, datastoreService.getDefaultDatastore, akkaServiceClient)
    openStackIdentityV3Handler = new OpenStackIdentityV3Handler(config, identityAPI)
    initialized = true
  }
}
