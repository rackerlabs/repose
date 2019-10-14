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

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.{KeystoneV2Result, Reject}
import org.openrepose.filters.keystonev2.KeystoneV2Common._
import org.openrepose.filters.keystonev2.config._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object KeystoneV2Authorization extends StrictLogging {

  val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = {
    case Failure(e: InvalidTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
    case Failure(e: UnauthorizedEndpointException) => Reject(SC_FORBIDDEN, Some(e.getMessage))
    case Failure(e: UnparsableTenantException) => Reject(SC_UNAUTHORIZED, Some(e.getMessage))
  }

  // TODO: Break this into separate authorizeTenants/authorizeEndpoints calls to separate concerns
  def doAuthorization(config: KeystoneV2Config, request: HttpServletRequestWrapper, userTenantToRolesMap: TenantToRolesMap, endpoints: => Try[EndpointsData]): AuthorizationInfo = {
    // NOTE TO REVIEWER: Behavior change -- pre-authorization is determined based on all roles, not just those matching the tenant (since that is part of authorization).
    //                   This change actually makes the behavior consistent with documentation.
    if (isUserPreAuthed(config.getPreAuthorizedRoles, userTenantToRolesMap.values.flatten.toSeq)) {
      AuthorizationPassed(userTenantToRolesMap, Set.empty)
    } else {
      val validateTenant = Option(config.getTenantHandling.getValidateTenant)
      val requestTenants = validateTenant
        .map(getRequestTenants(_, request))
        .getOrElse(Set.empty)
      val tenantPrefixes = validateTenant
        .map(_.getStripTokenTenantPrefixes)
        .flatMap(Option.apply)
        .map(_.split('/'))
        .getOrElse(Array.empty)
      val scopedTenantToRolesMap = validateTenant
        .map(_ => getScopedTenantToRolesMap(tenantPrefixes, userTenantToRolesMap, requestTenants))
      val matchedTenants = (scopedTenantToRolesMap.getOrElse(Map.empty) - DomainRoleTenantKey).keySet
      val tenantAuthorization = validateTenant
        .map(_ => authorizeTenant(tenantPrefixes, matchedTenants, requestTenants))
        .getOrElse(Success(Unit))

      val endpointAuthorization = Option(config.getRequireServiceEndpoint)
        .map(requiredEndpoint => authorizeEndpoints(requiredEndpoint, endpoints))
        .getOrElse(Success(Unit))

      tenantAuthorization.flatMap(_ => endpointAuthorization) match {
        case Success(_) => AuthorizationPassed(scopedTenantToRolesMap.getOrElse(userTenantToRolesMap), matchedTenants)
        case Failure(e) => AuthorizationFailed(scopedTenantToRolesMap.getOrElse(userTenantToRolesMap), matchedTenants, e)
      }
    }
  }

  // TODO: Rather than throwing an exception, this method should return a Try.
  // NOTE: Throwing an exception currently works because this method is always called in a [[Try]] stack.
  @throws[UnparsableTenantException]
  def getRequestTenants(config: ValidateTenantType, request: HttpServletRequestWrapper): Set[String] = {
    val requestTenants = config.getHeaderExtractionName.asScala.toSet.flatMap(request.getSplittableHeaderScala)
    if (requestTenants.nonEmpty) {
      requestTenants
    } else {
      throw UnparsableTenantException("Could not parse tenant from the configured header")
    }
  }

  // NOTE: Replaces the tenant and roles scoping methods. For the roles scoping, also accounts for tenant prefixes, which was not previously the case.
  def getScopedTenantToRolesMap(prefixes: Array[String], userTenantToRolesMap: TenantToRolesMap, requestTenants: Set[String]): TenantToRolesMap = {
    userTenantToRolesMap filterKeys { userTenant =>
      userTenant == DomainRoleTenantKey || requestTenants.exists { requestTenant =>
        prefixableTenantEquals(prefixes, userTenant, requestTenant)
      }
    }
  }

  def isUserPreAuthed(config: RolesList, roles: Seq[String]): Boolean = {
    Option(config) exists { preAuthedRoles =>
      preAuthedRoles.getRole.asScala.intersect(roles).nonEmpty
    }
  }

  def authorizeTenant(prefixes: Array[String], userTenants: Set[String], requestTenants: Set[String]): Try[Unit.type] = {
    logger.trace("Validating tenant")

    val allTenantsMatch = requestTenants forall { requestTenant =>
      userTenants exists { userTenant =>
        prefixableTenantEquals(prefixes, userTenant, requestTenant)
      }
    }

    if (allTenantsMatch) Success(Unit)
    else Failure(InvalidTenantException("A tenant from the configured header does not match any of the user's tenants"))
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

  private def prefixableTenantEquals(prefixes: Array[String], prefixableTenant: String, otherTenant: String): Boolean = {
    prefixableTenant == otherTenant || prefixes.exists(prefix =>
      prefixableTenant.startsWith(prefix) && prefixableTenant.substring(prefix.length) == otherTenant)
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
