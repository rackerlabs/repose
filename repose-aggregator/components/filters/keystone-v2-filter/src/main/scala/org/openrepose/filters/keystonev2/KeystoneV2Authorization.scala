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
import org.openrepose.filters.keystonev2.KeystoneV2Common._
import org.openrepose.filters.keystonev2.config._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object KeystoneV2Authorization extends LazyLogging {

  val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = {
    case Failure(e: InvalidTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
    case Failure(e: UnauthorizedEndpointException) => Reject(SC_FORBIDDEN, Some(e.getMessage))
    case Failure(e: UnparsableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
  }

  // TODO: Break this into separate authorizeTenants/authorizeEndpoints calls to separate concerns
  def doAuthorization(config: KeystoneV2Config, request: HttpServletRequestWrapper, tenantToRolesMap: TenantToRolesMap, endpoints: => Try[EndpointsData]): AuthorizationInfo = {
    // NOTE TO REVIEWER: Behavior change -- pre-authorization is determined based on all roles, not just those matching the tenant (since that is part of authorization).
    //                   This change actually makes the behavior consistent with documentation.
    if (isUserPreAuthed(config.getPreAuthorizedRoles, tenantToRolesMap.values.flatten.toSeq)) {
      AuthorizationPassed(tenantToRolesMap, Set.empty)
    } else {
      val validateTenant = Option(config.getTenantHandling.getValidateTenant)
      val tenantsToMatch = validateTenant
        .map(getRequestTenants(_, request))
        .getOrElse(Set.empty)
      val scopedTenantToRolesMap = validateTenant
        .map(getScopedTenantToRolesMap(_, tenantsToMatch, tenantToRolesMap))
      // val scopedRoles = scopedTenantToRolesMap.getOrElse(tenantToRolesMap).values.flatten.toSet
      val matchedTenants = (scopedTenantToRolesMap.getOrElse(Map.empty) - DomainRoleTenantKey).keySet
      val tenantAuthorization = validateTenant
        .map(authorizeTenant(_, tenantsToMatch, matchedTenants))
        .getOrElse(Success(Unit))

      val endpointAuthorization = Option(config.getRequireServiceEndpoint)
        .map(requiredEndpoint => authorizeEndpoints(requiredEndpoint, endpoints))
        .getOrElse(Success(Unit))

      tenantAuthorization.flatMap(_ => endpointAuthorization) match {
        case Success(_) => AuthorizationPassed(scopedTenantToRolesMap.getOrElse(tenantToRolesMap), matchedTenants)
        case Failure(e) => AuthorizationFailed(scopedTenantToRolesMap.getOrElse(tenantToRolesMap), matchedTenants, e)
      }
    }
  }

  // TODO: Rather than throwing an exception, this method should return a Try.
  // NOTE: Throwing an exception currently works because this method is always called in a [[Try]] stack.
  @throws[UnparsableTenantException]
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

  // NOTE: Replaces the tenant and roles scoping methods. For the roles scoping, also accounts for tenant prefixes, which was not previously the case.
  def getScopedTenantToRolesMap(config: ValidateTenantType, tenantsToMatch: Set[String], tenantToRolesMap: TenantToRolesMap): TenantToRolesMap = {
    val prefixes = Option(config.getStripTokenTenantPrefixes).map(_.split('/')).getOrElse(Array.empty[String])
    tenantToRolesMap filterKeys { tenant =>
      tenant == DomainRoleTenantKey || tenantsToMatch.exists(tenantToMatch =>
        tenant == tenantToMatch || prefixes.exists(prefix =>
          tenant.startsWith(prefix) && tenant.substring(prefix.length) == tenantToMatch))
    }
  }

  def isUserPreAuthed(config: RolesList, roles: Seq[String]): Boolean = {
    Option(config) exists { preAuthedRoles =>
      preAuthedRoles.getRole.asScala.intersect(roles).nonEmpty
    }
  }

  def authorizeTenant(config: ValidateTenantType, tenantsToMatch: Set[String], userTenants: Set[String]): Try[Unit.type] = {
    logger.trace("Validating tenant")

    val prefixes = Option(config.getStripTokenTenantPrefixes).map(_.split('/')).getOrElse(Array.empty[String])
    val allTenantsMatch = tenantsToMatch forall { tenantToMatch =>
      userTenants exists { tenant =>
        tenant == tenantToMatch || prefixes.exists(prefix =>
          tenant.startsWith(prefix) && tenant.substring(prefix.length) == tenantToMatch)
      }
    }

    if (allTenantsMatch) Success(Unit)
    else Failure(InvalidTenantException("A tenant from the URI and/or the configured header does not match any of the user's tenants"))
  }

  def authorizeEndpoints(config: ServiceEndpointType, maybeEndpoints: => Try[EndpointsData]): Try[Unit.type] = {
    logger.trace("Authorizing endpoints")

    maybeEndpoints flatMap { endpoints =>
      val requiredEndpointModel =
        Endpoint(
          publicURL = config.getPublicUrl,
          name = Option(config.getName),
          endpointType = Option(config.getType),
          region = Option(config.getRegion)
        )

      if (endpoints.vector.exists(_.meetsRequirement(requiredEndpointModel))) Success(Unit)
      else Failure(UnauthorizedEndpointException("User did not have the required endpoint"))
    }
  }

  sealed trait AuthorizationInfo {
    def scopedTenantToRolesMap: TenantToRolesMap
    def matchedTenants: Set[String]
  }
  case class AuthorizationPassed(scopedTenantToRolesMap: TenantToRolesMap, matchedTenants: Set[String]) extends AuthorizationInfo
  case class AuthorizationFailed(scopedTenantToRolesMap: TenantToRolesMap, matchedTenants: Set[String], exception: Throwable) extends AuthorizationInfo

  abstract class AuthorizationException(message: String, cause: Throwable) extends Exception(message, cause)

  case class UnauthorizedEndpointException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class InvalidTenantException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

  case class UnparsableTenantException(message: String, cause: Throwable = null) extends AuthorizationException(message, cause)

}
