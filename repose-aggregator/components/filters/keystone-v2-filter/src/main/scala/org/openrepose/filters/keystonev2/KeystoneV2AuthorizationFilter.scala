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
package org.openrepose.filters.keystonev2

import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR

import com.fasterxml.jackson.core.JsonProcessingException
import org.openrepose.commons.utils.http.OpenStackServiceHeader.{ROLES, TENANT_ID, TENANT_ROLES_MAP}
import org.openrepose.commons.utils.http.PowerApiHeader.X_CATALOG
import org.openrepose.commons.utils.json.JsonHeaderHelper
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.commons.utils.string.Base64Helper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.{KeystoneV2Result, Reject}
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.{AuthorizationException, AuthorizationFailed, AuthorizationPassed, doAuthorization}
import org.openrepose.filters.keystonev2.KeystoneV2Common._
import org.openrepose.filters.keystonev2.config.KeystoneV2Config
import play.api.libs.json.{JsResultException, Json}

import scala.util.{Failure, Success, Try}

@Named
class KeystoneV2AuthorizationFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractKeystoneV2Filter[KeystoneV2Config](configurationService) {

  import KeystoneV2AuthorizationFilter._

  override val DEFAULT_CONFIG = "keystone-v2-authorization.cfg.xml"
  override val SCHEMA_LOCATION = "/META-INF/schema/config/keystone-v2-authorization.xsd"

  override val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = {
    KeystoneV2Authorization.handleFailures orElse {
      case Failure(e@(_: MissingTenantToRolesMapException |
                      _: MissingEndpointsException |
                      _: InvalidTenantToRolesMapException |
                      _: InvalidEndpointsException)) =>
        Reject(SC_INTERNAL_SERVER_ERROR, Some(e.getMessage))
    }
  }

  override def doAuth(request: HttpServletRequestWrapper): Try[Unit.type] = {
    getTenantToRolesMap(request) flatMap { tenantToRolesMap =>
      doAuthorization(configuration, request, tenantToRolesMap, getEndpoints(request)) match {
        case AuthorizationPassed(scopedTenantToRolesMap, matchedTenants) if scopedTenantToRolesMap.nonEmpty =>
          scopeTenantIdHeader(request, matchedTenants)
          scopeRolesHeader(request, scopedTenantToRolesMap.values.flatten.toSet)
          scopeTenantToRolesMapHeader(request, scopedTenantToRolesMap)
          Success(Unit)
        case AuthorizationPassed(_, _) => Success(Unit)
        case AuthorizationFailed(_, _, exception) => Failure(exception)
      }
    }
  }

  def getTenantToRolesMap(request: HttpServletRequest): Try[TenantToRolesMap] = {
    logger.trace("Getting the tenant-to-roles mapping from a request header")

    Try {
      Option(request.getHeader(TENANT_ROLES_MAP))
        .map(JsonHeaderHelper.jsonHeaderToValue)
        .get
        .as[TenantToRolesMap]
    } recover {
      case nsee: NoSuchElementException =>
        throw MissingTenantToRolesMapException(s"$TENANT_ROLES_MAP header does not exist", nsee)
      case e@(_: IllegalArgumentException | _: JsonProcessingException | _: JsResultException) =>
        throw InvalidTenantToRolesMapException(s"$TENANT_ROLES_MAP header value is not a valid tenant-to-roles map representation", e)
    }
  }

  def getEndpoints(request: HttpServletRequest): Try[EndpointsData] = {
    logger.trace("Getting the endpoints from a request header")

    Try {
      val jsonString = Option(request.getHeader(X_CATALOG))
        .map(Base64Helper.base64DecodeUtf8)
        .get
      val json = Json.parse(jsonString)

      (json \ "endpoints").validate[Vector[Endpoint]]
        .map(EndpointsData(jsonString, _))
        .getOrElse(throw new JsonProcessingException("Could not validate endpoints JSON") {})
    } recover {
      case nsee: NoSuchElementException =>
        throw MissingEndpointsException(s"$X_CATALOG header does not exist", nsee)
      case e@(_: IllegalArgumentException | _: JsonProcessingException) =>
        throw InvalidEndpointsException(s"$X_CATALOG header value is not a valid endpoints representation", e)
    }
  }

  def scopeTenantIdHeader(request: HttpServletRequestWrapper, matchedTenants: Set[String]): Unit = {
    logger.trace("Scoping the tenant ID request header")

    val sendAllTenantIds = configuration.getTenantHandling.isSendAllTenantIds
    val matchedTenantQuality = Option(configuration.getTenantHandling.getSendTenantIdQuality).map(_.getValidatedTenantQuality)

    (sendAllTenantIds, matchedTenantQuality) match {
      case (true, Some(quality)) =>
        matchedTenants.foreach(request.appendHeader(TENANT_ID, _, quality))
      case (true, None) =>
        matchedTenants.foreach(request.appendHeader(TENANT_ID, _))
      case (false, Some(quality)) =>
        request.removeHeader(TENANT_ID)
        matchedTenants.foreach(request.appendHeader(TENANT_ID, _, quality))
      case (false, None) =>
        request.removeHeader(TENANT_ID)
        matchedTenants.foreach(request.appendHeader(TENANT_ID, _))
    }
  }

  def scopeRolesHeader(request: HttpServletRequestWrapper, roles: Set[String]): Unit = {
    logger.trace("Scoping the roles request header")

    Option(configuration.getTenantHandling.getValidateTenant).filter(_.isEnableLegacyRolesMode) getOrElse {
      request.removeHeader(ROLES)
      roles.foreach(request.appendHeader(ROLES, _))
    }
  }

  def scopeTenantToRolesMapHeader(request: HttpServletRequestWrapper, tenantToRolesMap: TenantToRolesMap): Unit = {
    logger.trace("Scoping the tenant-to-roles mapping request header")

    if (!configuration.getTenantHandling.isSendAllTenantIds) {
      request.removeHeader(TENANT_ROLES_MAP)
      request.addHeader(TENANT_ROLES_MAP, JsonHeaderHelper.anyToJsonHeader(tenantToRolesMap))
    }
  }
}

object KeystoneV2AuthorizationFilter {

  case class MissingTenantToRolesMapException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class MissingEndpointsException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class InvalidTenantToRolesMapException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class InvalidEndpointsException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

}
