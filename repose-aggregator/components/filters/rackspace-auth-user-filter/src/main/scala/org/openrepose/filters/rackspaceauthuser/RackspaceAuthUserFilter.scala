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

import java.util.concurrent.atomic.AtomicReference
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.{OpenStackServiceHeader, PowerApiHeader}
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService

@Named
class RackspaceAuthUserFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractConfiguredFilter[RackspaceAuthUserConfig](configurationService) with LazyLogging {

  override final val DEFAULT_CONFIG = "rackspace-auth-user.cfg.xml"
  override final val SCHEMA_LOCATION = "/META-INF/config/schema/rackspace-auth-user-configuration.xsd"

  private val handler: AtomicReference[RackspaceAuthUserHandler] = new AtomicReference[RackspaceAuthUserHandler]()

  override def doWork(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    if (httpServletRequest.getMethod != "POST") {
      // this filter only operates on POST requests that have a body to parse
      filterChain.doFilter(servletRequest, servletResponse)
    } else {
      val rawRequestInputStream = httpServletRequest.getInputStream
      val requestInputStream =
        if (rawRequestInputStream.markSupported) rawRequestInputStream
        else new BufferedServletInputStream(rawRequestInputStream)
      val wrappedRequest = new HttpServletRequestWrapper(httpServletRequest, requestInputStream)
      handler.get.parseUserGroupFromInputStream(wrappedRequest.getInputStream, wrappedRequest.getContentType) foreach { rackspaceAuthUserGroup =>
        rackspaceAuthUserGroup.domain.foreach { domainVal =>
          wrappedRequest.addHeader(PowerApiHeader.DOMAIN.toString, domainVal)
        }
        wrappedRequest.addHeader(PowerApiHeader.USER.toString, rackspaceAuthUserGroup.user, rackspaceAuthUserGroup.quality)
        wrappedRequest.addHeader(OpenStackServiceHeader.USER_NAME.toString, rackspaceAuthUserGroup.user)
        wrappedRequest.addHeader(PowerApiHeader.GROUPS.toString, rackspaceAuthUserGroup.group, rackspaceAuthUserGroup.quality)
      }

      filterChain.doFilter(wrappedRequest, servletResponse)
    }
  }

  override def configurationUpdated(config: RackspaceAuthUserConfig): Unit = {
    handler.set(new RackspaceAuthUserHandler(config))
    super.configurationUpdated(config)
  }
}
