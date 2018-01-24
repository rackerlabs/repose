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

import java.util.Base64
import javax.inject.{Inject, Named}
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR

import com.fasterxml.jackson.core.JsonProcessingException
import org.openrepose.commons.utils.http.OpenStackServiceHeader.{ROLES, TENANT_ID}
import org.openrepose.commons.utils.http.PowerApiHeader.X_CATALOG
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.{KeystoneV2Result, Reject}
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.{AuthorizationException, AuthorizationFailed, AuthorizationPassed, doAuthorization}
import org.openrepose.filters.keystonev2.KeystoneV2Common.{Endpoint, EndpointsData, TokenRequestAttributeName, ValidToken}
import org.openrepose.filters.keystonev2.config.KeystoneV2Config
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}

@Named
class KeystoneV2AuthorizationFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractKeystoneV2Filter[KeystoneV2Config](configurationService) {

  import KeystoneV2AuthorizationFilter._

  override val DEFAULT_CONFIG = "keystone-v2-authorization.cfg.xml"
  override val SCHEMA_LOCATION = "/META-INF/schema/config/keystone-v2-authorization.xsd"

  override val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = {
    KeystoneV2Authorization.handleFailures orElse {
      case Failure(e@(_: MissingTokenException |
                      _: MissingEndpointsException |
                      _: InvalidTokenException |
                      _: InvalidEndpointsException)) =>
        Reject(SC_INTERNAL_SERVER_ERROR, Some(e.getMessage))
    }
  }

  override def doAuth(request: HttpServletRequestWrapper): Try[Unit.type] = {
    // TODO: Stop using request attributes. Stop using the token. Just use the tenant to roles map, tenant ids, and roles headers.
    getToken(request) flatMap { token =>
      doAuthorization(configuration, request, token, getEndpoints(request)) match {
        case AuthorizationPassed(scopedToken, matchedTenants) if matchedTenants.nonEmpty =>
          scopeTenantIdHeader(request, matchedTenants)
          scopeRolesHeader(request, scopedToken)
          Success(Unit)
        case AuthorizationPassed(_, _) => Success(Unit)
        case AuthorizationFailed(_, _, exception) => Failure(exception)
      }
    }
  }

  def getToken(request: ServletRequest): Try[ValidToken] = {
    Try {
      Option(request.getAttribute(TokenRequestAttributeName)).get.asInstanceOf[ValidToken]
    } recover {
      case nsee: NoSuchElementException => throw MissingTokenException("Token request attribute does not exist", nsee)
      case cce: ClassCastException => throw InvalidTokenException("Token request attribute is not a valid token", cce)
    }
  }

  def getEndpoints(request: HttpServletRequest): Try[EndpointsData] = {
    Try {
      val jsonString = Option(request.getHeader(X_CATALOG))
        .map(Base64.getDecoder.decode)
        .map(new String(_))
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
    val tenantHandling = Option(configuration.getTenantHandling)
    val sendAllTenantIds = tenantHandling.exists(_.isSendAllTenantIds)
    val matchedTenantQuality = tenantHandling.map(_.getSendTenantIdQuality).flatMap(Option.apply).map(_.getUriTenantQuality)

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

  def scopeRolesHeader(request: HttpServletRequestWrapper, scopedToken: ValidToken): Unit = {
    request.removeHeader(ROLES)
    scopedToken.roles.map(_.name).foreach(request.appendHeader(ROLES, _))
  }
}

object KeystoneV2AuthorizationFilter {

  case class MissingTokenException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class MissingEndpointsException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class InvalidTokenException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class InvalidEndpointsException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

}
