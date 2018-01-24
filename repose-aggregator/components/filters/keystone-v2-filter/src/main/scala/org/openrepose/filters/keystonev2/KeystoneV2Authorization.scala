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
    case Failure(e: UnparsableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
  }

  // TODO: Take tenants to roles mapping instead of token to calculate scoped roles.
  def doAuthorization(config: KeystoneV2Config, request: HttpServletRequestWrapper, validToken: ValidToken, endpoints: => Try[EndpointsData]): AuthorizationInfo = {
    lazy val tenantsToMatch = getRequestTenants(config.getTenantHandling.getValidateTenant, request)

    val tenantScopedRoles = getTenantScopedRoles(config.getTenantHandling.getValidateTenant, tenantsToMatch, validToken.roles)
    val userIsPreAuthed = isUserPreAuthed(config.getPreAuthorizedRoles, tenantScopedRoles)
    val scopedRolesToken = if (userIsPreAuthed) validToken else validToken.copy(roles = tenantScopedRoles)
    val doTenantCheck = shouldAuthorizeTenant(config.getTenantHandling.getValidateTenant, userIsPreAuthed)
    val matchedTenants = getMatchingTenants(config.getTenantHandling.getValidateTenant, tenantsToMatch, doTenantCheck, scopedRolesToken)
    val authResult = authorizeTenant(doTenantCheck, tenantsToMatch, matchedTenants) flatMap { _ =>
      authorizeEndpoints(config.getRequireServiceEndpoint, userIsPreAuthed, endpoints)
    }
    authResult match {
      case Success(_) => AuthorizationPassed(scopedRolesToken, matchedTenants)
      case Failure(exception) => AuthorizationFailed(scopedRolesToken, matchedTenants, exception)
    }
  }

  def getRequestTenants(config: ValidateTenantType, request: HttpServletRequestWrapper): Set[String] = {
    Option(config).map(validateTenantConfig =>
      validateTenantConfig.getUriExtractionRegexAndHeaderExtractionName.asScala.toSet.flatMap((extraction: ExtractionType) => extraction match {
        case headerExtraction: HeaderExtractionType =>
          request.getSplittableHeaderScala(headerExtraction.getValue)
        case uriExtraction: UriExtractionType =>
          val uriRegex = uriExtraction.getValue.r
          request.getRequestURI match {
            case uriRegex(tenantId, _*) => Some(tenantId)
            case _ => None
          }
        case _ =>
          logger.error("An unexpected tenant extraction type was encountered")
          None
      })
    ).filter(_.nonEmpty).getOrElse(throw UnparsableTenantException("Could not parse tenant from the URI and/or the configured header"))
  }

  def getTenantScopedRoles(config: ValidateTenantType, tenantsToMatch: => Set[String], roles: Seq[Role]): Seq[Role] = {
    Option(config) match {
      case Some(validateTenant) if !validateTenant.isEnableLegacyRolesMode =>
        roles.filter(role =>
          role.tenantId.forall(roleTenantId =>
            tenantsToMatch.contains(roleTenantId)))
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

  def getMatchingTenants(config: ValidateTenantType, tenantsToMatch: => Set[String], doTenantCheck: Boolean, validToken: ValidToken): Set[String] = {
    if (doTenantCheck) {
      val tokenTenants = validToken.defaultTenantId.toSet ++ validToken.tenantIds
      val prefixes = Option(config.getStripTokenTenantPrefixes).map(_.split('/')).getOrElse(Array.empty[String])
      tenantsToMatch map { tenantToMatch =>
        tokenTenants find { tokenTenant =>
          tokenTenant == tenantToMatch || prefixes.exists(prefix =>
            tokenTenant.startsWith(prefix) && tokenTenant.substring(prefix.length) == tenantToMatch)
        }
      } filter (_.nonEmpty) map (_.get)
    } else {
      Set.empty
    }
  }

  def authorizeTenant(doTenantCheck: Boolean, tenantsToMatch: => Set[String], matchedTenants: Set[String]): Try[Unit.type] = {
    logger.trace("Validating tenant")

    if (doTenantCheck) {
      if (matchedTenants.size == tenantsToMatch.size) Success(Unit)
      else Failure(InvalidTenantException("A tenant from the URI and/or the configured header does not match any of the user's tenants"))
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
    def matchedTenants: Set[String]
  }
  case class AuthorizationPassed(scopedToken: ValidToken, matchedTenants: Set[String]) extends AuthorizationInfo
  case class AuthorizationFailed(scopedToken: ValidToken, matchedTenants: Set[String], exception: Throwable) extends AuthorizationInfo

  abstract class AuthorizationException(message: String, cause: Throwable) extends Exception(message, cause)

  case class UnauthorizedEndpointException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class InvalidTenantException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class UnparsableTenantException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

}
