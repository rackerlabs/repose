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
import java.util.Base64
import javax.servlet._
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.OpenStackServiceHeader.{TENANT_ID, TENANT_ROLES_MAP}
import org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.tenantculling.TenantCullingFilter.{packMap, unpackMap}
import play.api.libs.json.Json

class TenantCullingFilter extends Filter with LazyLogging {

  @Override
  @throws[ServletException]
  override def init(filterConfig: FilterConfig): Unit = {
    //do nothing
  }

  @Override
  @throws[IOException]
  @throws[ServletException]
  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
    val response = servletResponse.asInstanceOf[HttpServletResponse]
    val rolesMap = Option(request.getHeader(TENANT_ROLES_MAP))
    val relevantRoles = request.getSplittableHeaderScala(RELEVANT_ROLES).map(_.split('/').head)
    rolesMap match {
      case Some(tenantToRolesMap) =>
        try {
          val culledMap = unpackMap(tenantToRolesMap).filter({ case(tenant, roles) => roles.toList.intersect(relevantRoles).nonEmpty })
          request.replaceHeader(TENANT_ROLES_MAP, packMap(culledMap))
          request.replaceHeader(TENANT_ID, culledMap.keySet.filterNot("repose/domain/roles".equals).mkString(","))
          chain.doFilter(request, response)
        } catch {
          case e@(_: IllegalArgumentException | _: JsonParseException) =>
            logger.error("A problem occurred while trying to parse the role map.", e)
            response.sendError(SC_INTERNAL_SERVER_ERROR)
        }
      case None =>
        logger.debug("Tenant to roles map header not found")
        response.sendError(SC_INTERNAL_SERVER_ERROR)
    }
  }

  @Override override def destroy(): Unit = {
  }
}

object TenantCullingFilter {
  type TenantToRolesMap = Map[String, Set[String]]

  def packMap(tenantToRolesMap: TenantToRolesMap): String = {
    val tenantToRolesJson = Json.stringify(Json.toJson(tenantToRolesMap))
    Base64.getEncoder.encodeToString(tenantToRolesJson.getBytes)
  }

  def unpackMap(tenantToRolesMap: String): TenantToRolesMap = {
    Json.parse(new String(Base64.getDecoder.decode(tenantToRolesMap))).validate[TenantToRolesMap].get
  }
}
