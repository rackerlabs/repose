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

import javax.servlet.http.HttpServletResponse.{SC_FORBIDDEN, SC_UNAUTHORIZED}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.{KeystoneV2Result, Reject}
import org.openrepose.filters.keystonev2.KeystoneV2Common.{Endpoint, EndpointsData, Role, ValidToken}
import org.openrepose.filters.keystonev2.config._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object KeystoneV2Authorization extends LazyLogging {

  val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = {
    case Failure(e: InvalidTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
    case Failure(e: UnauthorizedEndpointException) => Reject(SC_FORBIDDEN, Some(e.getMessage))
    case Failure(e: UnparseableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
  }

  // TODO: Take tenants to roles mapping instead of token to calculate scoped roles.
  def doAuthorization(config: KeystoneV2Config, request: HttpServletRequestWrapper, validToken: ValidToken, endpoints: => Try[EndpointsData]): AuthorizationInfo = {
    lazy val tenantToMatch = getRequestTenant(config.getTenantHandling.getValidateTenant, request)

    val tenantScopedRoles = getTenantScopedRoles(config.getTenantHandling.getValidateTenant, tenantToMatch, validToken.roles)
    val userIsPreAuthed = isUserPreAuthed(config.getPreAuthorizedRoles, tenantScopedRoles)
    val scopedRolesToken = if (userIsPreAuthed) validToken else validToken.copy(roles = tenantScopedRoles)
    val doTenantCheck = shouldAuthorizeTenant(config.getTenantHandling.getValidateTenant, userIsPreAuthed)
    val matchedTenant = getMatchingTenant(config.getTenantHandling.getValidateTenant, tenantToMatch, doTenantCheck, scopedRolesToken)
    val authResult = authorizeTenant(doTenantCheck, matchedTenant) flatMap { _ =>
      authorizeEndpoints(config.getRequireServiceEndpoint, userIsPreAuthed, endpoints)
    }
    authResult match {
      case Success(_) => AuthorizationPassed(scopedRolesToken, matchedTenant)
      case Failure(exception) => AuthorizationFailed(scopedRolesToken, matchedTenant, exception)
    }
  }

  // TODO: Switch to getRequestTenants (plural) and include all tenants extracted. This is a behavior change.
  def getRequestTenant(config: ValidateTenantType, request: HttpServletRequestWrapper): String = {
    Option(config).flatMap(validateTenantConfig =>
      Option(validateTenantConfig.getHeaderExtractionName).flatMap(headerName =>
        request.getSplittableHeaderScala(headerName).headOption
      ).orElse(
        Option(validateTenantConfig.getUriExtractionRegex).flatMap(uriExtractionRegexList =>
          uriExtractionRegexList.asScala.toStream.map(_.r).flatMap(uriExtractionRegex =>
            request.getRequestURI match {
              case uriExtractionRegex(tenantId, _*) => Option(tenantId)
              case _ => None
            }
          ).headOption
        )
      )
    ).getOrElse(throw UnparseableTenantException("Could not parse tenant from the URI and/or the configured header"))
  }

  def getTenantScopedRoles(config: ValidateTenantType, tenantToMatch: => String, roles: Seq[Role]): Seq[Role] = {
    Option(config) match {
      case Some(validateTenant) if !validateTenant.isEnableLegacyRolesMode =>
        roles.filter(role =>
          role.tenantId.forall(roleTenantId =>
            roleTenantId.equals(tenantToMatch)))
      case _ =>
        roles
    }
  }

  def isUserPreAuthed(config: RolesList, roles: Seq[Role]): Boolean = {
    Option(config) exists { preAuthedRoles =>
      preAuthedRoles.getRole.asScala.intersect(roles.map(_.name)).nonEmpty
    }
  }

  def shouldAuthorizeTenant(config: ValidateTenantType, preAuthed: Boolean): Boolean = {
    Option(config).exists(_ => !preAuthed)
  }

  def getMatchingTenant(config: ValidateTenantType, tenantToMatch: => String, doTenantCheck: Boolean, validToken: ValidToken): Option[String] = {
    if (doTenantCheck) {
      val tokenTenants = validToken.defaultTenantId.toSet ++ validToken.tenantIds
      val prefixes = Option(config.getStripTokenTenantPrefixes).map(_.split('/')).getOrElse(Array.empty[String])
      tokenTenants find { tokenTenant =>
        tokenTenant.equals(tenantToMatch) || prefixes.exists(prefix =>
          tokenTenant.startsWith(prefix) && tokenTenant.substring(prefix.length).equals(tenantToMatch)
        )
      }
    } else {
      None
    }
  }

  def authorizeTenant(doTenantCheck: Boolean, matchedTenant: Option[String]): Try[Unit.type] = {
    logger.trace("Validating tenant")

    if (doTenantCheck) {
      if (matchedTenant.isDefined) Success(Unit)
      else Failure(InvalidTenantException("Tenant from URI does not match any of the tenants associated with the provided token"))
    } else {
      Success(Unit)
    }
  }

  def authorizeEndpoints(config: ServiceEndpointType, userIsPreAuthed: Boolean, maybeEndpoints: => Try[EndpointsData]): Try[Unit.type] = {
    Option(config) match {
      case Some(configuredEndpoint) =>
        logger.trace("Authorizing endpoints")

        maybeEndpoints flatMap { endpoints =>
          lazy val requiredEndpoint =
            Endpoint(
              publicURL = configuredEndpoint.getPublicUrl,
              name = Option(configuredEndpoint.getName),
              endpointType = Option(configuredEndpoint.getType),
              region = Option(configuredEndpoint.getRegion)
            )

          if (userIsPreAuthed || endpoints.vector.exists(_.meetsRequirement(requiredEndpoint))) Success(Unit)
          else Failure(UnauthorizedEndpointException("User did not have the required endpoint"))
        }
      case None => Success(Unit)
    }
  }

  sealed trait AuthorizationInfo {
    def scopedToken: ValidToken
    def matchedTenant: Option[String]
  }
  case class AuthorizationPassed(scopedToken: ValidToken, matchedTenant: Option[String]) extends AuthorizationInfo
  case class AuthorizationFailed(scopedToken: ValidToken, matchedTenant: Option[String], exception: Throwable) extends AuthorizationInfo

  case class UnauthorizedEndpointException(message: String, cause: Throwable = null) extends Exception(message, cause)

  case class InvalidTenantException(message: String, cause: Throwable = null) extends Exception(message, cause)

  case class UnparseableTenantException(message: String, cause: Throwable = null) extends Exception(message, cause)

}
