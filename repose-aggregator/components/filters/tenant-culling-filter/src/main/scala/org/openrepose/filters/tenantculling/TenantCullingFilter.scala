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
package org.openrepose.filters.tenantculling

import java.io.IOException
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse.{SC_INTERNAL_SERVER_ERROR, SC_UNAUTHORIZED}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID
import org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES
import org.openrepose.commons.utils.io.ObjectSerializer
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.filters.keystonev2.{KeystoneRequestHandler, KeystoneV2Filter}
import org.slf4j.{Logger, LoggerFactory}

@Named class TenantCullingFilter @Inject()(datastoreService: DatastoreService) extends Filter {
  private val log: Logger = LoggerFactory.getLogger(classOf[TenantCullingFilter])
  private val datastore = datastoreService.getDefaultDatastore
  private val objectSerializer = new ObjectSerializer(getClass.getClassLoader)

  @Override
  @throws[ServletException]
  override def init(filterConfig: FilterConfig): Unit = {
    //do nothing
  }

  @Override
  @throws[IOException]
  @throws[ServletException]
  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    def logNamedSeq(name: String, seq: Seq[Any]): Unit = {
      log.trace("{} :", name)
      seq.foreach(log.trace(" - {}", _))
    }

    def withDefaultTenant(token: KeystoneRequestHandler.ValidToken, tenants: Seq[String]): Seq[String] = {
      logNamedSeq("Tenant ID's", tenants)
      if (token.defaultTenantId.isDefined) {
        log.trace("Adding Default:")
        log.trace(" - {}", token.defaultTenantId.get)
        tenants :+ token.defaultTenantId.get
      } else {
        tenants
      }
    }

    val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
    val response = servletResponse.asInstanceOf[HttpServletResponse]
    val cacheKey = request.getHeader(KeystoneV2Filter.AuthTokenKey)
    val relevantRoles = request.getSplittableHeaderScala(RELEVANT_ROLES)
    if (cacheKey != null) {
      try {
        val token = objectSerializer.readObject(objectSerializer.writeObject(datastore.get(cacheKey))).asInstanceOf[KeystoneRequestHandler.ValidToken]
        if (token != null) {
          logNamedSeq("Token Roles", token.roles)
          logNamedSeq("Relevant Roles", relevantRoles)
          val tenants: Seq[String] = token.roles
            .filter(role => relevantRoles.contains(role.name))
            .filter(_.tenantId.isDefined)
            .map(_.tenantId.get)
          request.removeHeader(TENANT_ID)
          val withDefault = withDefaultTenant(token, tenants)
          log.debug("Adding {}:", TENANT_ID)
          withDefault.foreach { tenant =>
            log.debug(" - {}", tenant)
            request.addHeader(TENANT_ID, tenant)
          }
          chain.doFilter(request, response)
        }
        else {
          log.debug("Cache miss for key: {}", cacheKey)
          response.sendError(SC_UNAUTHORIZED)
        }
      } catch {
        case cnfe: ClassNotFoundException =>
          log.error("This shouldn't have been possible, somehow the item that came back from datastore doesn't match a class available in the current classloader", cnfe)
          response.sendError(SC_INTERNAL_SERVER_ERROR)
      }
    }
    else {
      log.debug("Cache key header not found")
      response.sendError(SC_UNAUTHORIZED)
    }
  }

  @Override override def destroy(): Unit = {
  }
}
