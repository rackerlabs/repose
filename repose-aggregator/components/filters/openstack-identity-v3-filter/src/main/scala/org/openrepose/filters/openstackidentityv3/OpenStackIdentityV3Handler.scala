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
package org.openrepose.filters.openstackidentityv3

import java.util.{Calendar, GregorianCalendar}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.codec.binary.Base64
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.servlet.filter.FilterAction
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityV3Config, WhiteList}
import org.openrepose.filters.openstackidentityv3.json.spray.IdentityJsonProtocol._
import org.openrepose.filters.openstackidentityv3.objects._
import org.openrepose.filters.openstackidentityv3.utilities._
import org.springframework.http.HttpHeaders
import spray.json._

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class OpenStackIdentityV3Handler(identityConfig: OpenstackIdentityV3Config, identityAPI: OpenStackIdentityV3API)
  extends HttpDelegationManager with LazyLogging {

  private val identityServiceUri = identityConfig.getOpenstackIdentityService.getUri
  private val forwardGroups = identityConfig.isForwardGroups
  private val forwardCatalog = identityConfig.isForwardCatalog
  private val delegatingWithQuality = Option(identityConfig.getDelegating).map(_.getQuality)
  private val projectIdUriRegex = Option(identityConfig.getValidateProjectIdInUri).map(_.getRegex.r)
  private val projectIdPrefixes = Try(identityConfig.getValidateProjectIdInUri.getStripTokenProjectPrefixes.split('/')).getOrElse(Array.empty[String])
  private val bypassProjectIdCheckRoles = Option(identityConfig.getRolesWhichBypassProjectIdCheck).map(_.getRole.asScala.toList)
  private val configuredServiceEndpoint = Option(identityConfig.getServiceEndpoint) map { serviceEndpoint =>
    Endpoint(id = "configured-endpoint",
      url = serviceEndpoint.getUrl,
      name = Option(serviceEndpoint.getName),
      interface = Option(serviceEndpoint.getInterface),
      region = Option(serviceEndpoint.getRegion))
  }

  def handleRequest(request: HttpServletRequestWrapper, response: HttpServletResponse): FilterAction = {
    var filterAction = FilterAction.PASS

    def delegateOrElse(responseCode: Int, message: String)(f: => Unit) {
      delegatingWithQuality match {
        case Some(quality) =>
          buildDelegationHeaders(responseCode, "openstack-identity-v3", message, quality) foreach { case (key, values) =>
            values foreach { value =>
              request.addHeader(key, value)
            }
            filterAction = FilterAction.PROCESS_RESPONSE
            // Note: The response status code must be set to a < 500 so that the request will be routed appropriately by the PowerFilterChain isResponseOk method.
            response.setStatus(HttpServletResponse.SC_OK)
          }
        case None =>
          f
      }
    }

    def authServiceOverLimit(e: IdentityServiceOverLimitException): Unit = {
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
      var retry = e.getRetryAfter
      if (retry == null) {
        val retryCalendar = new GregorianCalendar
        retryCalendar.add(Calendar.SECOND, 5)
        retry = new HttpDate(retryCalendar.getTime).toRFC1123
      }
      response.addHeader(HttpHeaders.RETRY_AFTER, retry)
    }

    // Check if the request URI is whitelisted and pass it along if so
    if (isUriWhitelisted(request.getRequestURI, identityConfig.getWhiteList)) {
      logger.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")
      filterAction = FilterAction.PASS
    } else {
      // Set the default behavior for this filter
      filterAction = FilterAction.RETURN
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

      // Track whether or not a failure has occurred so that we can stop checking the request after we know it is bad
      var failureInValidation = false

      // Extract the tracing GUID from the request
      val tracingHeader = Option(request.getHeader(CommonHttpHeader.TRACE_GUID.toString))

      // Attempt to validate the request token with the Identity service
      val token = authenticate(request, tracingHeader) match {
        case Success(tokenObject) =>
          Some(tokenObject)
        case Failure(e: InvalidSubjectTokenException) =>
          failureInValidation = true
          delegateOrElse(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage) {
            response.setHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
          }
          None
        case Failure(e: IdentityServiceOverLimitException) =>
          failureInValidation = true
          delegateOrElse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage) {
            authServiceOverLimit(e)
          }
          None
        case Failure(e) =>
          failureInValidation = true
          logger.error(e.getMessage, e)
          delegateOrElse(response.getStatus, e.getMessage) {}
          None
      }

      // Attempt to check the project ID if configured to do so
      if (!failureInValidation && !isProjectIdValid(request.getRequestURI, token.get)) {
        failureInValidation = true
        delegateOrElse(HttpServletResponse.SC_UNAUTHORIZED, "Invalid project ID for token: " + token.get) {
          response.setHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
          filterAction = FilterAction.RETURN
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        }
      }

      // Attempt to authorize the token against a configured endpoint
      if (!failureInValidation && !isAuthorized(token.get)) {
        failureInValidation = true
        delegateOrElse(HttpServletResponse.SC_FORBIDDEN, "Invalid endpoints for token: " + token.get) {
          filterAction = FilterAction.RETURN
          response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        }
      }

      // Attempt to fetch groups if configured to do so
      val userGroups = if (!failureInValidation && forwardGroups) {
        token.get.user.id map { userId =>
          identityAPI.getGroups(userId, tracingHeader) match {
            case Success(groupsList) =>
              groupsList.map(_.name)
            case Failure(e: IdentityServiceOverLimitException) =>
              failureInValidation = true
              delegateOrElse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage) {
                authServiceOverLimit(e)
              }
              List.empty[String]
            case Failure(e) =>
              failureInValidation = true
              logger.error(e.getMessage, e)
              delegateOrElse(response.getStatus, e.getMessage) {}
              List.empty[String]
          }
        } getOrElse {
          failureInValidation = true
          logger.warn("The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.")
          delegateOrElse(response.getStatus,
            "The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.") {}
          List.empty[String]
        }
      } else {
        List.empty[String]
      }

      // If all validation succeeds, pass the request and set headers
      if (!failureInValidation) {
        filterAction = FilterAction.PASS
        // Note: The response status code must be set to a < 500 so that the request will be routed appropriately by the PowerFilterChain isResponseOk method.
        response.setStatus(HttpServletResponse.SC_OK)

        // Set the appropriate headers
        request.replaceHeader(OpenStackIdentityV3Headers.X_TOKEN_EXPIRES, token.get.expires_at)
        request.replaceHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION.toString, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified
        token.get.user.name.foreach { user =>
          request.addHeader(OpenStackIdentityV3Headers.X_USER_NAME.toString, user)
          request.addHeader(PowerApiHeader.USER.toString, user, 1.0)
        }
        token.get.roles.foreach { roles =>
          request.addHeader(OpenStackIdentityV3Headers.X_ROLES, roles.map(_.name) mkString ",")
        }
        token.get.user.id.foreach { id =>
          request.replaceHeader(OpenStackIdentityV3Headers.X_USER_ID.toString, id)
        }
        token.get.user.rax_default_region.foreach { defaultRegion =>
          request.replaceHeader(OpenStackIdentityV3Headers.X_DEFAULT_REGION.toString, defaultRegion)
        }
        token.get.project.foreach { project =>
          project.name.foreach { projectName =>
            request.replaceHeader(OpenStackIdentityV3Headers.X_PROJECT_NAME.toString, projectName)
          }
        }
        projectIdUriRegex match {
          case Some(regex) =>
            val defaultProjectId = token.flatMap(_.project.flatMap(_.id))
            val roles = token.flatMap(_.roles).getOrElse(List[Role]())
            val uriProjectId = extractProjectIdFromUri(regex, request.getRequestURI)
            writeProjectHeader(defaultProjectId, roles, uriProjectId, identityConfig.isSendAllProjectIds,
              identityConfig.isSendProjectIdQuality, request)
          case None =>
            val defaultProjectId = token.flatMap(_.project.flatMap(_.id))
            val roles = token.flatMap(_.roles).getOrElse(List[Role]())
            writeProjectHeader(defaultProjectId, roles, None, identityConfig.isSendAllProjectIds,
              identityConfig.isSendProjectIdQuality, request)
        }
        token.get.rax_impersonator.foreach { impersonator =>
          impersonator.id.foreach(request.replaceHeader(OpenStackIdentityV3Headers.X_IMPERSONATOR_ID.toString, _))
          impersonator.name.foreach(request.replaceHeader(OpenStackIdentityV3Headers.X_IMPERSONATOR_NAME.toString, _))
        }
        if (forwardCatalog) {
          token.get.catalog.foreach(catalog => request.replaceHeader(PowerApiHeader.X_CATALOG.toString, base64Encode(catalog.toJson.compactPrint)))
        }
        if (forwardGroups) {
          userGroups.foreach(group => request.addHeader(PowerApiHeader.GROUPS.toString, group, 1.0))
        }
      }

      // Forward potentially unauthorized requests if configured to do so, or denote authorized requests
      if (delegatingWithQuality.isDefined) {
        if (!failureInValidation) {
          request.replaceHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Confirmed.name)
        } else {
          logger.debug("Forwarding indeterminate request")
          request.replaceHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Indeterminate.name)
          request.replaceHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified
          filterAction = FilterAction.PROCESS_RESPONSE
        }
      }
    }

    filterAction
  }

  private def authenticate(request: HttpServletRequest, tracingHeader: Option[String] = None): Try[AuthenticateResponse] = {
    Option(request.getHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN)) match {
      case Some(subjectToken) =>
        identityAPI.validateToken(subjectToken, tracingHeader)
      case None =>
        logger.error("No X-Subject-Token present -- a subject token was not provided to validate")
        Failure(new InvalidSubjectTokenException("A subject token was not provided to validate"))
    }
  }

  private def isAuthorized(authResponse: AuthenticateResponse) = {
    configuredServiceEndpoint forall { configuredEndpoint =>
      val tokenEndpoints = authResponse.catalog.map(catalog => catalog.flatMap(service => service.endpoints)).getOrElse(List.empty[Endpoint])

      containsRequiredEndpoint(tokenEndpoints, configuredEndpoint)
    }
  }

  private def containsRequiredEndpoint(endpointsList: List[Endpoint], endpointRequirement: Endpoint) =
    endpointsList exists (endpoint => endpoint.meetsRequirement(endpointRequirement))

  private def writeProjectHeader(defaultProject: Option[String], roles: List[Role], projectFromUri: Option[String],
                                 writeAll: Boolean, sendQuality: Boolean, request: HttpServletRequestWrapper) {
    lazy val projectsFromRoles = (roles.collect { case Role(_, _, Some(projectId), _, _, _) => projectId } :::
      roles.collect { case Role(_, _, _, Some(raxId), _, _) => raxId }).toSet

    if (writeAll && sendQuality) {
      defaultProject.foreach(request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _, 1.0))
      projectsFromRoles foreach { rolePid =>
        if (!defaultProject.exists(defaultPid => rolePid.equals(defaultPid))) {
          request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, rolePid, 0.5)
        }
      }
    } else if (writeAll && !sendQuality) {
      defaultProject.foreach(request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _))
      projectsFromRoles foreach { rolePid =>
        if (!defaultProject.exists(defaultPid => rolePid.equals(defaultPid))) {
          request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, rolePid)
        }
      }
    } else if (!writeAll && sendQuality) {
      projectFromUri match {
        case Some(projectId) =>
          request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, projectId, 1.0)
        case None =>
          defaultProject.foreach(request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _, 1.0))
      }
    } else {
      projectFromUri match {
        case Some(projectId) =>
          request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, projectId)
        case None =>
          defaultProject.foreach(request.addHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _))
      }
    }
  }

  private def isProjectIdValid(requestUri: String, token: AuthenticateResponse): Boolean = {
    projectIdUriRegex match {
      case Some(regex) =>
        // Check whether or not this user should bypass project ID validation
        val userRoles = token.roles.getOrElse(List[Role]()).map(_.name)
        val bypassProjectIdCheck = hasIgnoreEnabledRole(bypassProjectIdCheckRoles.getOrElse(List.empty[String]), userRoles)

        if (bypassProjectIdCheck) {
          true
        } else {
          // Extract the project ID from the URI
          val extractedProjectId = extractProjectIdFromUri(regex, requestUri)

          // Bind the default project ID, if available
          val defaultProjectId = token.project.map(_.id).getOrElse(None)

          // Attempt to match the extracted project ID against the project IDs in the token
          extractedProjectId match {
            case Some(projectId) => projectMatches(projectId, defaultProjectId, token.roles.getOrElse(List[Role]()))
            case None => false
          }
        }
      case None => true
    }
  }

  private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[String]): Boolean =
    userRoles.exists(userRole => ignoreProjectRoles.exists(ignoreRole => ignoreRole.equals(userRole)))

  private def projectMatches(projectFromUri: String, defaultProjectId: Option[String], roles: List[Role]): Boolean = {
    val allProjectIds = defaultProjectId.toSet ++
      roles.filter(_.project_id.isDefined).map(_.project_id.get) ++
      roles.filter(_.rax_project_id.isDefined).map(_.rax_project_id.get)

    allProjectIds.exists { pid =>
      pid.equals(projectFromUri) || projectIdPrefixes.exists { prefix =>
        pid.startsWith(prefix) && pid.substring(prefix.length).equals(projectFromUri)
      }
    }
  }

  private def extractProjectIdFromUri(projectIdRegex: Regex, uri: String): Option[String] =
    projectIdRegex.findFirstMatchIn(uri).map(regexMatch => regexMatch.group(1))

  private def base64Encode(s: String) =
    Base64.encodeBase64String(s.getBytes)

  private def isUriWhitelisted(requestUri: String, whiteList: WhiteList) = {
    val convertedWhiteList = Option(whiteList).map(_.getUriPattern.asScala.toList).getOrElse(List.empty[String])
    convertedWhiteList.exists(requestUri.matches)
  }

  def handleResponse(response: HttpServletResponse): Unit = {
    val responseStatus = response.getStatus
    logger.debug("OpenStack Identity v3 Handling Response. Incoming status code: " + responseStatus)

    /// The WWW Authenticate header can be used to communicate to the client
    // (since we are a proxy) how to correctly authenticate itself
    val wwwAuthenticateHeader = Option(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString))

    responseStatus match {
      // NOTE: We should only mutate the WWW-Authenticate header on a
      // 401 (unauthorized) or 403 (forbidden) response from the origin service
      case HttpServletResponse.SC_FORBIDDEN | HttpServletResponse.SC_UNAUTHORIZED =>
        // If in the case that the origin service supports delegated authentication
        // we should then communicate to the client how to authenticate with us
        if (wwwAuthenticateHeader.isDefined && wwwAuthenticateHeader.get.toLowerCase.contains(OpenStackIdentityV3Headers.X_DELEGATED.toLowerCase)) {
          val responseAuthHeaderValues = response.getHeaders(CommonHttpHeader.WWW_AUTHENTICATE.toString).asScala.toList
          val valuesWithoutDelegated = responseAuthHeaderValues.filterNot(_.equalsIgnoreCase(OpenStackIdentityV3Headers.X_DELEGATED))
          val valuesWithKeystone = ("Keystone uri=" + identityServiceUri) :: valuesWithoutDelegated

          valuesWithKeystone.headOption foreach { headValue =>
            response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, headValue)
            valuesWithKeystone.tail foreach { remainingValue =>
              response.addHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, remainingValue)
            }
          }
        } else {
          // In the case where authentication has failed and we did not receive
          // a delegated WWW-Authenticate header, this means that our own authentication
          // with the origin service has failed and must then be communicated as
          // a 500 (internal server error) to the client
          logger.error("Authentication with the origin service has failed.")
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
      case HttpServletResponse.SC_NOT_IMPLEMENTED =>
        if (wwwAuthenticateHeader.isDefined && wwwAuthenticateHeader.get.contains(OpenStackIdentityV3Headers.X_DELEGATED)) {
          logger.error("Repose authentication component is configured to forward unauthorized requests, but the origin service does not support delegated mode.")
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED)
        }
      case _ =>
        logger.trace("Response from origin service requires no additional processing. Passing it along.")
    }

    logger.debug("OpenStack Identity v3 Handling Response. Outgoing status code: " + response.getStatus)
  }
}
