package org.openrepose.filters.openstackidentityv3

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.codec.binary.Base64
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector, HeaderManager}
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityV3Config, WhiteList}
import org.openrepose.filters.openstackidentityv3.json.spray.IdentityJsonProtocol._
import org.openrepose.filters.openstackidentityv3.objects._
import org.openrepose.filters.openstackidentityv3.utilities._
import spray.json._

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class OpenStackIdentityV3Handler(identityConfig: OpenstackIdentityV3Config, identityAPI: OpenStackIdentityV3API)
  extends AbstractFilterLogicHandler with HttpDelegationManager with LazyLogging {

  private val identityServiceUri = identityConfig.getOpenstackIdentityService.getUri
  private val forwardGroups = identityConfig.isForwardGroups
  private val forwardCatalog = identityConfig.isForwardCatalog
  private val delegatingWithQuality = Option(identityConfig.getDelegating).map(_.getQuality)
  private val projectIdUriRegex = Option(identityConfig.getValidateProjectIdInUri).map(_.getRegex.r)
  private val bypassProjectIdCheckRoles = Option(identityConfig.getRolesWhichBypassProjectIdCheck).map(_.getRole.asScala.toList)
  private val configuredServiceEndpoint = Option(identityConfig.getServiceEndpoint) map { serviceEndpoint =>
    Endpoint(id = "configured-endpoint",
      url = serviceEndpoint.getUrl,
      name = Option(serviceEndpoint.getName),
      interface = Option(serviceEndpoint.getInterface),
      region = Option(serviceEndpoint.getRegion))
  }

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector: FilterDirector = new FilterDirectorImpl()

    def delegateOrElse(responseCode: Int, message: String)(f: => Unit) {
      delegatingWithQuality match {
        case Some(quality) =>
          buildDelegationHeaders(responseCode, "openstack-identity-v3", message, quality) foreach { case (key, values) =>
            filterDirector.requestHeaderManager.appendHeader(key, values: _*)
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
            filterDirector.setResponseStatusCode(200) // Note: The response status code must be set to a non-500 so that the request will be routed appropriately.
          }
        case None =>
          f
      }
    }

    // Check if the request URI is whitelisted and pass it along if so
    if (isUriWhitelisted(request.getRequestURI, identityConfig.getWhiteList)) {
      logger.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")
      filterDirector.setFilterAction(FilterAction.PASS)
    } else {
      val requestHeaderManager = filterDirector.requestHeaderManager

      // Set the default behavior for this filter
      filterDirector.setFilterAction(FilterAction.RETURN)
      filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)

      // Track whether or not a failure has occurred so that we can stop checking the request after we know it is bad
      var failureInValidation = false

      // Attempt to validate the request token with the Identity service
      val token = authenticate(request) match {
        case Success(tokenObject) =>
          Some(tokenObject)
        case Failure(e: InvalidSubjectTokenException) =>
          failureInValidation = true
          delegateOrElse(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage) {
            filterDirector.responseHeaderManager.putHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED)
          }
          None
        case Failure(e) =>
          failureInValidation = true
          logger.error(e.getMessage)
          delegateOrElse(filterDirector.getResponseStatusCode, e.getMessage) {}
          None
      }

      // Attempt to check the project ID if configured to do so
      if (!failureInValidation && !isProjectIdValid(request.getRequestURI, token.get)) {
        failureInValidation = true
        delegateOrElse(HttpServletResponse.SC_UNAUTHORIZED, "Invalid project ID for token: " + token.get) {
          filterDirector.responseHeaderManager.putHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
          filterDirector.setFilterAction(FilterAction.RETURN)
          filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED)
        }
      }

      // Attempt to authorize the token against a configured endpoint
      if (!failureInValidation && !isAuthorized(token.get)) {
        failureInValidation = true
        delegateOrElse(HttpServletResponse.SC_FORBIDDEN, "Invalid endpoints for token: " + token.get) {
          filterDirector.setFilterAction(FilterAction.RETURN)
          filterDirector.setResponseStatusCode(HttpServletResponse.SC_FORBIDDEN)
        }
      }

      // Attempt to fetch groups if configured to do so
      val userGroups = if (!failureInValidation && forwardGroups) {
        token.get.user.id map { userId =>
          identityAPI.getGroups(userId) match {
            case Success(groupsList) =>
              groupsList.map(_.name)
            case Failure(e) =>
              failureInValidation = true
              logger.error(e.getMessage)
              delegateOrElse(filterDirector.getResponseStatusCode, e.getMessage) {}
              List[String]()
          }
        } getOrElse {
          failureInValidation = true
          logger.warn("The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.")
          delegateOrElse(filterDirector.getResponseStatusCode,
            "The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.") {}
          List[String]()
        }
      } else {
        List[String]()
      }

      // If all validation succeeds, pass the request and set headers
      if (!failureInValidation) {
        filterDirector.setFilterAction(FilterAction.PASS)

        // Set the appropriate headers
        requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_TOKEN_EXPIRES, token.get.expires_at)
        requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION.toString, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified
        token.get.user.name.foreach(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_USER_NAME.toString, _))
        token.get.roles.foreach { roles =>
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_ROLES, roles.map(_.name) mkString ",")
        }
        token.get.user.id.foreach { id =>
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_USER_ID.toString, id)
          requestHeaderManager.appendHeader(PowerApiHeader.USER.toString, id, 1.0)
        }
        token.get.user.rax_default_region.foreach { defaultRegion =>
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_DEFAULT_REGION.toString, defaultRegion)
        }
        token.get.project.foreach { project =>
          project.name.foreach { projectName =>
            requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_PROJECT_NAME.toString, projectName)
          }
        }
        projectIdUriRegex match {
          case Some(regex) =>
            val defaultProjectId = token.flatMap(_.project.flatMap(_.id))
            val roles = token.flatMap(_.roles).getOrElse(List[Role]())
            val uriProjectId = extractProjectIdFromUri(regex, request.getRequestURI)
            writeProjectHeader(defaultProjectId, roles, uriProjectId, identityConfig.isSendAllProjectIds,
              identityConfig.isSendProjectIdQuality, filterDirector.requestHeaderManager)
          case None =>
            val defaultProjectId = token.flatMap(_.project.flatMap(_.id))
            val roles = token.flatMap(_.roles).getOrElse(List[Role]())
            writeProjectHeader(defaultProjectId, roles, None, identityConfig.isSendAllProjectIds,
              identityConfig.isSendProjectIdQuality, filterDirector.requestHeaderManager)
        }
        token.get.rax_impersonator.foreach { impersonator =>
          impersonator.id.foreach(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IMPERSONATOR_ID.toString, _))
          impersonator.name.foreach(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IMPERSONATOR_NAME.toString, _))
        }
        if (forwardCatalog) {
          token.get.catalog.foreach(catalog => requestHeaderManager.putHeader(PowerApiHeader.X_CATALOG.toString, base64Encode(catalog.toJson.compactPrint)))
        }
        if (forwardGroups) {
          userGroups.foreach(group => requestHeaderManager.appendHeader(PowerApiHeader.GROUPS.toString, group + ";q=1.0"))
        }
      }

      // Forward potentially unauthorized requests if configured to do so, or denote authorized requests
      if (delegatingWithQuality.isDefined) {
        if (!failureInValidation) {
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Confirmed.name)
        } else {
          logger.debug("Forwarding indeterminate request")
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Indeterminate.name)
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified
          filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
        }
      }
    }

    filterDirector
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    logger.debug("OpenStack Identity v3 Handling Response. Incoming status code: " + response.getStatus)
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    val responseStatus = response.getStatus
    filterDirector.setResponseStatusCode(responseStatus)

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

          filterDirector.responseHeaderManager.putHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, valuesWithKeystone: _*)
        } else {
          // In the case where authentication has failed and we did not receive
          // a delegated WWW-Authenticate header, this means that our own authentication
          // with the origin service has failed and must then be communicated as
          // a 500 (internal server error) to the client
          logger.error("Authentication with the origin service has failed.")
          filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
      case HttpServletResponse.SC_NOT_IMPLEMENTED =>
        if (wwwAuthenticateHeader.isDefined && wwwAuthenticateHeader.get.contains(OpenStackIdentityV3Headers.X_DELEGATED)) {
          logger.error("Repose authentication component is configured to forward unauthorized requests, but the origin service does not support delegated mode.")
          filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        } else {
          filterDirector.setResponseStatusCode(HttpServletResponse.SC_NOT_IMPLEMENTED)
        }
      case _ =>
        logger.trace("Response from origin service requires no additional processing. Passing it along.")
    }

    logger.debug("OpenStack Identity v3 Handling Response. Outgoing status code: " + filterDirector.getResponseStatusCode)
    filterDirector
  }

  private def authenticate(request: HttpServletRequest): Try[AuthenticateResponse] = {
    Option(request.getHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN)) match {
      case Some(subjectToken) =>
        identityAPI.validateToken(subjectToken)
      case None =>
        Failure(new InvalidSubjectTokenException("A subject token was not provided to validate"))
    }
  }

  private def isAuthorized(authResponse: AuthenticateResponse) = {
    configuredServiceEndpoint forall { configuredEndpoint =>
      val tokenEndpoints = authResponse.catalog.map(catalog => catalog.map(service => service.endpoints).flatten).getOrElse(List.empty[Endpoint])

      containsRequiredEndpoint(tokenEndpoints, configuredEndpoint)
    }
  }

  private def containsRequiredEndpoint(endpointsList: List[Endpoint], endpointRequirement: Endpoint) =
    endpointsList exists (endpoint => endpoint.meetsRequirement(endpointRequirement))

  private def writeProjectHeader(defaultProject: Option[String], roles: List[Role], projectFromUri: Option[String],
                                 writeAll: Boolean, sendQuality: Boolean, headerManager: HeaderManager) {
    lazy val projectsFromRoles = (roles.collect { case Role(_, _, Some(projectId), _, _, _) => projectId} :::
      roles.collect { case Role(_, _, _, Some(raxId), _, _) => raxId}).toSet

    if (writeAll && sendQuality) {
      defaultProject.foreach(headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _, 1.0))
      projectsFromRoles foreach { rolePid =>
        if (!defaultProject.exists(defaultPid => rolePid.equals(defaultPid))) {
          headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, rolePid, 0.5)
        }
      }
    } else if (writeAll && !sendQuality) {
      defaultProject.foreach(headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _))
      projectsFromRoles foreach { rolePid =>
        if (!defaultProject.exists(defaultPid => rolePid.equals(defaultPid))) {
          headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, rolePid)
        }
      }
    } else if (!writeAll && sendQuality) {
      projectFromUri map {
        headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _, 1.0)
      } orElse {
        defaultProject.map(headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _, 1.0))
      }
    } else {
      projectFromUri map {
        headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _)
      } orElse {
        defaultProject.map(headerManager.appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, _))
      }
    }
  }

  private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[String]): Boolean =
    userRoles.exists(userRole => ignoreProjectRoles.exists(ignoreRole => ignoreRole.equals(userRole)))

  private def isProjectIdValid(requestUri: String, token: AuthenticateResponse): Boolean = {
    projectIdUriRegex match {
      case Some(regex) =>
        // Check whether or not this user should bypass project ID validation
        val userRoles = token.roles.getOrElse(List[Role]()).map(_.name)
        val bypassProjectIdCheck = hasIgnoreEnabledRole(bypassProjectIdCheckRoles.getOrElse(List[String]()), userRoles)

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

  private def extractProjectIdFromUri(projectIdRegex: Regex, uri: String): Option[String] =
    projectIdRegex.findFirstMatchIn(uri).map(regexMatch => regexMatch.group(1))

  private def projectMatches(projectFromUri: String, defaultProjectId: Option[String], roles: List[Role]): Boolean = {
    val defaultIdMatches = defaultProjectId.exists(_.equals(projectFromUri))
    val keystoneRolesIdMatches = roles.exists(role =>
      role.project_id.exists(rolePID =>
        rolePID.equals(projectFromUri)
      )
    )
    val raxRolesIdMatches = roles.exists(role =>
      role.rax_project_id.exists(rolePID =>
        rolePID.equals(projectFromUri)
      )
    )

    defaultIdMatches || keystoneRolesIdMatches || raxRolesIdMatches
  }

  private def base64Encode(s: String) =
    Base64.encodeBase64String(s.getBytes)

  private def isUriWhitelisted(requestUri: String, whiteList: WhiteList) = {
    val convertedWhiteList = Option(whiteList).map(_.getUriPattern.asScala.toList).getOrElse(List.empty[String])
    convertedWhiteList.filter(requestUri.matches).nonEmpty
  }
}
