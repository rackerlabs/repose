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
package org.openrepose.filters.rackspaceauthuser

import javax.inject.{Inject, Named}
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate
import org.openrepose.core.services.config.ConfigurationService

@Named
class RackspaceAuthUserFilter @Inject() (configurationService: ConfigurationService) extends Filter with LazyLogging {
  private final val DEFAULT_CONFIG = "rackspace-auth-user.cfg.xml"

  private var config: String = _
  private var handlerFactory: RackspaceAuthUserHandlerFactory = _

  override def init(filterConfig: FilterConfig): Unit = {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info(s"Initializing RackspaceAuthUserFilter using config $config")
    handlerFactory = new RackspaceAuthUserHandlerFactory()
    val xsdURL = getClass.getResource("/META-INF/config/schema/rackspace-auth-user-configuration.xsd")
    // TODO: Clean up the asInstanceOf below, if possible?
    configurationService.subscribeTo(filterConfig.getFilterName,
      config,
      xsdURL,
      handlerFactory.asInstanceOf[UpdateListener[RackspaceAuthUserConfig]],
      classOf[RackspaceAuthUserConfig])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(handlerFactory.newHandler)
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(config, handlerFactory)
  }
}
