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
  private val configuredServiceEndpoint = Option(identityConfig.getServiceEndpoint) map { serviceEndpoint =>
    Endpoint(id = "configured-endpoint",
      url = serviceEndpoint.getUrl,
      name = Option(serviceEndpoint.getName),
      interface = Option(serviceEndpoint.getInterface),
      region = Option(serviceEndpoint.getRegion))
  }

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val filterDirector: FilterDirector = new FilterDirectorImpl()

    if (isUriWhitelisted(request.getRequestURI, identityConfig.getWhiteList)) {
      LOG.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")
      filterDirector.setFilterAction(FilterAction.PASS)
    } else {
      val requestHeaderManager = filterDirector.requestHeaderManager()

      // Set the default behavior for this filter
      filterDirector.setFilterAction(FilterAction.RETURN)
      filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)

      var authSuccess = false
      authenticate(request) match {
        case Success(tokenObject) =>
          authSuccess = true

          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_TOKEN_EXPIRES, tokenObject.expires_at)
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION.toString, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified (not in-scope)
          tokenObject.user.name.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_USER_NAME.toString, _))
          tokenObject.roles.map { roles =>
            requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_ROLES, roles.map(_.name) mkString ",")
          }
          tokenObject.user.id.map { id =>
            requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_USER_ID.toString, id)
            requestHeaderManager.appendHeader(PowerApiHeader.USER.toString, id, 1.0)
          }
          tokenObject.project.map { project =>
            project.id.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_PROJECT_ID.toString, _))
            project.name.map(requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_PROJECT_NAME.toString, _))
          }
          if (forwardCatalog) {
            tokenObject.catalog.map(catalog => requestHeaderManager.putHeader(PowerApiHeader.X_CATALOG.toString, base64Encode(catalog.toJson.compactPrint)))
          }
          if (forwardGroups) {
            tokenObject.user.id map { userId: String =>
              identityAPI.getGroups(userId) map { groupsList: List[Group] =>
                groupsList map { group: Group =>
                  requestHeaderManager.appendHeader(PowerApiHeader.GROUPS.toString, group.name + ";q=1.0")
                }
              }
            } orElse {
              LOG.warn("The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.")
              None
            }
          }
          // TODO: Set X-Impersonator-Name, need to check response for impersonator (out of scope)
          // TODO: Set X-Impersonator-Id, same as above
          // TODO: Set X-Default-Region, may require another API call? Doesn't seem to be returned in a token

          if (isAuthorized(tokenObject)) {
            filterDirector.setFilterAction(FilterAction.PASS)
          } else {
            filterDirector.setFilterAction(FilterAction.RETURN)
            filterDirector.setResponseStatus(HttpStatusCode.FORBIDDEN)
          }
        case Failure(e: InvalidSubjectTokenException) =>
          filterDirector.responseHeaderManager.putHeader(OpenStackIdentityV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + identityServiceUri)
          filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
        case Failure(e) =>
          LOG.error(e.getMessage)
      }

      if (forwardUnauthorizedRequests) {
        if (authSuccess) {
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Confirmed.name)
        } else {
          LOG.debug("Forwarding indeterminate request")
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_IDENTITY_STATUS, IdentityStatus.Indeterminate.name)
          requestHeaderManager.putHeader(OpenStackIdentityV3Headers.X_AUTHORIZATION, OpenStackIdentityV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified (not in-scope)
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
    val projectsFromRoles: Set[String] = if (writeAll) roles.map({ role => role.project_id.get}).toSet else Set.empty
    def projects: Set[String] = projectsFromRoles + projectFromUri

    filterDirector.requestHeaderManager().appendHeader("X-PROJECT-ID", projects.toArray: _*)
  }

  private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[Role]): Boolean = true

  private def isProjectIdValid(requestUri: String, token: AuthenticateResponse): Boolean = {
    projectIdUriRegex match {
      case Some(regex) =>
        // Extract the project ID from the URI
        val extractedProjectId = extractProjectIdFromUri(regex, requestUri)

        // Bind the default project ID, if available
        val defaultProjectId = token.project.map(_.id).getOrElse(None)

        // Attempt to match the extracted project ID against the project IDs in the token
        extractedProjectId match {
          case Some(projectId) => projectMatches(projectId, defaultProjectId, token.roles.getOrElse(List[Role]()))
          case None => false
        }
      case None => true
    }
  }

  private def extractProjectIdFromUri(projectIdRegex: Regex, uri: String): Option[String] =
    projectIdRegex.findFirstMatchIn(uri).map(regexMatch => regexMatch.group(1))

  private def projectMatches(projectFromUri: String, defaultProjectId: Option[String], roles: List[Role]): Boolean = {
    defaultProjectId.exists(_.equals(projectFromUri)) ||
      roles.exists(role =>
        role.project_id.exists(rolePID =>
          rolePID.equals(projectFromUri)
        )
      )
  }

  private def base64Encode(s: String) =
    Base64.encodeBase64String(s.getBytes)

  private def isUriWhitelisted(requestUri: String, whiteList: WhiteList) = {
    val convertedWhiteList = Option(whiteList).map(_.getUriPattern.asScala.toList).getOrElse(List.empty[String])
    convertedWhiteList.filter(requestUri.matches).nonEmpty
  }
}
