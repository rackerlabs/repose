package com.rackspace.papi.components.openstack.identity.v3

import javax.servlet.http.HttpServletRequest

import com.rackspace.papi.commons.util.http._
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.openstack.identity.v3.config.{OpenstackIdentityV3Config, WhiteList}
import com.rackspace.papi.components.openstack.identity.v3.json.spray.IdentityJsonProtocol._
import com.rackspace.papi.components.openstack.identity.v3.objects._
import com.rackspace.papi.components.openstack.identity.v3.utilities._
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

class OpenStackIdentityV3Handler(identityConfig: OpenstackIdentityV3Config, identityAPI: OpenStackIdentityV3API)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[OpenStackIdentityV3Handler])

  private val identityServiceUri = identityConfig.getOpenstackIdentityService.getUri
  private val forwardGroups = identityConfig.isForwardGroups
  private val forwardCatalog = identityConfig.isForwardCatalog
  private val forwardUnauthorizedRequests = identityConfig.isForwardUnauthorizedRequests
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

    // Check if the request URI is whitelisted and pass it along if so
    if (isUriWhitelisted(request.getRequestURI, identityConfig.getWhiteList)) {
      LOG.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")
      filterDirector.setFilterAction(FilterAction.PASS)
    } else {
      val requestHeaderManager = filterDirector.requestHeaderManager

      // Set the default behavior for this filter
      filterDirector.setFilterAction(FilterAction.RETURN)
      filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)

      // Track whether or not a failure has occurred so that we can stop checking the request after we know it is bad
      var failureInValidation = false

      // Attempt to validate the request token with the Identity service
      val token = authenticate(request) match {
        case Success(tokenObject) =>
          Some(tokenObject)
        case Failure(e: InvalidSubjectTokenException) =>
          failureInValidation = true
          filterDirector.responseHeaderManager.putHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
          filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
          None
        case Failure(e) =>
          failureInValidation = true
          LOG.error(e.getMessage)
          None
      }

      // Attempt to check the project ID if configured to do so
      if (!failureInValidation && !isProjectIdValid(request.getRequestURI, token.get)) {
        failureInValidation = true
        filterDirector.responseHeaderManager.putHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
        filterDirector.setFilterAction(FilterAction.RETURN)
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
      }

      // Attempt to authorize the token against a configured endpoint
      if (!failureInValidation && !isAuthorized(token.get)) {
        failureInValidation = true
        filterDirector.setFilterAction(FilterAction.RETURN)
        filterDirector.setResponseStatus(HttpStatusCode.FORBIDDEN)
      }

      // Attempt to fetch groups if configured to do so
      val userGroups = if (!failureInValidation && forwardGroups) {
        token.get.user.id map { userId =>
          identityAPI.getGroups(userId) match {
            case Success(groupsList) =>
              groupsList.map(_.name)
            case Failure(e) =>
              failureInValidation = true
              LOG.error(e.getMessage)
              List[String]()
          }
        } getOrElse {
          failureInValidation = true
          LOG.warn("The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.")
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
        token.get.user.name.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_USER_NAME.toString, _))
        token.get.roles.map { roles =>
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_ROLES, roles.map(_.name) mkString ",")
        }
        token.get.user.id.map { id =>
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_USER_ID.toString, id)
          requestHeaderManager.appendHeader(PowerApiHeader.USER.toString, id, 1.0)
        }
        token.get.user.rax_default_region.map { requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_DEFAULT_REGION.toString, _) }
        LOG.warn("**********************6")

        identityConfig.isSendAllProjectIds match {
          case false if projectIdUriRegex.isDefined => writeProjectHeader(extractProjectIdFromUri(projectIdUriRegex.get, request.getRequestURI).get, token.get.roles.get, writeAll = false, filterDirector)
          case false if token.flatMap(_.project).flatMap(_.id).isDefined =>
            token flatMap(_.project) map { project =>
              project.id.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_PROJECT_ID.toString, _))
              project.name.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_PROJECT_NAME.toString, _))
            }
          case false => None
          case true => writeProjectHeader(token flatMap(_.project) flatMap(_.id) get, token flatMap(_.roles) get, writeAll = true, filterDirector)
        }
        token.get.rax_impersonator.map { impersonator =>
          impersonator.id.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IMPERSONATOR_ID.toString,_))
          impersonator.name.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IMPERSONATOR_NAME.toString,_))
        }
        if (forwardCatalog) {
          token.get.catalog.map(catalog => requestHeaderManager.putHeader(PowerApiHeader.X_CATALOG.toString, base64Encode(catalog.toJson.compactPrint)))
        }
        if (forwardGroups) {
          userGroups.foreach(group => requestHeaderManager.appendHeader(PowerApiHeader.GROUPS.toString, group + ";q=1.0"))
        }
      }

      // Forward potentially unauthorized requests if configured to do so, or denote authorized requests
      if (forwardUnauthorizedRequests) {
        if (!failureInValidation) {
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Confirmed.name)
        } else {
          LOG.debug("Forwarding indeterminate request")
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Indeterminate.name)
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified
          filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
        }
      }
    }

    filterDirector
  }

  override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("OpenStack Identity v3 Handling Response. Incoming status code: " + response.getStatus)
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    val responseStatus = response.getStatus
    filterDirector.setResponseStatusCode(responseStatus)

    /// The WWW Authenticate header can be used to communicate to the client
    // (since we are a proxy) how to correctly authenticate itself
    val wwwAuthenticateHeader = Option(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString))

    HttpStatusCode.fromInt(responseStatus) match {
      // NOTE: We should only mutate the WWW-Authenticate header on a
      // 401 (unauthorized) or 403 (forbidden) response from the origin service
      case HttpStatusCode.FORBIDDEN | HttpStatusCode.UNAUTHORIZED =>
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
          LOG.error("Authentication with the origin service has failed.")
          filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)
        }
      case HttpStatusCode.NOT_IMPLEMENTED =>
        if (wwwAuthenticateHeader.isDefined && wwwAuthenticateHeader.get.contains(OpenStackIdentityV3Headers.X_DELEGATED)) {
          LOG.error("Repose authentication component is configured to forward unauthorized requests, but the origin service does not support delegated mode.")
          filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)
        } else {
          filterDirector.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED)
        }
      case _ =>
        LOG.trace("Response from origin service requires no additional processing. Passing it along.")
    }

    LOG.debug("OpenStack Identity v3 Handling Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
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

  private def writeProjectHeader(projectFromUri: String, roles: List[Role], writeAll: Boolean, filterDirector: FilterDirector) = {
    val projectsFromRoles:Set[String] = {
      if (writeAll){
        val roleList = for {
          r <- roles
          pid <- r.project_id
        } yield pid
        roleList.toSet
      } else {
        Set.empty
      }
    }
    def projects: Set[String] = projectsFromRoles + projectFromUri
    filterDirector.requestHeaderManager().appendHeader(OpenStackIdentityV3Headers.X_PROJECT_ID, projects.toArray: _*)
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
