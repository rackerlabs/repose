package com.rackspace.papi.components.keystone.v3

import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.rackspace.papi.commons.util.http._
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.keystone.v3.config.KeystoneV3Config
import com.rackspace.papi.components.keystone.v3.json.spray.IdentityJsonProtocol._
import com.rackspace.papi.components.keystone.v3.objects._
import com.rackspace.papi.components.keystone.v3.utilities._
import com.rackspace.papi.components.keystone.v3.utilities.exceptions._
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.apache.http.Header
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Random, Success, Try}

class KeystoneV3Handler(keystoneConfig: KeystoneV3Config, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[KeystoneV3Handler])
  private final val ADMIN_TOKEN_KEY = "ADMIN_TOKEN"
  private final val TOKEN_KEY_PREFIX = "TOKEN:"

  private lazy val keystoneServiceUri = keystoneConfig.getKeystoneService.getUri
  private lazy val tokenCacheTtl = keystoneConfig.getTokenCacheTimeout
  private lazy val cacheOffset = keystoneConfig.getCacheOffset
  private lazy val forwardUnauthorizedRequests = keystoneConfig.isForwardUnauthorizedRequests
  private lazy val datastore = datastoreService.getDefaultDatastore

  private[v3] var cachedAdminToken: String = null

  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    if (isUriWhitelisted(request.getRequestURI, Option(keystoneConfig.getWhiteList).map(_.getUriPattern.asScala.toList).getOrElse(List.empty[String]))) {
      LOG.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")
      val filterDirector: FilterDirector = new FilterDirectorImpl()
      filterDirector.setFilterAction(FilterAction.PASS)
      filterDirector
    } else {
      authenticate(request)
    }
  }

    override def handleResponse(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Keystone v3 Handling Response. Incoming status code: " + response.getStatus)
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
        if (wwwAuthenticateHeader.isDefined && wwwAuthenticateHeader.get.contains(KeystoneV3Headers.X_DELEGATED)) {
          filterDirector.responseHeaderManager.putHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, "Keystone uri=" + keystoneServiceUri)
        } else {
          // In the case where authentication has failed and we did not receive
          // a delegated WWW-Authenticate header, this means that our own authentication
          // with the origin service has failed and must then be communicated as
          // a 500 (internal server error) to the client
          LOG.error("Authentication with the origin service has failed.")
          filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)
        }
      case HttpStatusCode.NOT_IMPLEMENTED =>
        if (wwwAuthenticateHeader.isDefined && wwwAuthenticateHeader.get.contains(KeystoneV3Headers.X_DELEGATED)) {
          LOG.error("Repose authentication component is configured to forward unauthorized requests, but the origin service does not support delegated mode.")
          filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)
        } else {
          filterDirector.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED)
        }
      case _ => ()
    }

    LOG.debug("Keystone v3 Handling Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }

  private def authenticate(request: HttpServletRequest) = {
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()
    filterDirector.setFilterAction(FilterAction.RETURN)
    filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)

    Option(request.getHeader(KeystoneV3Headers.X_SUBJECT_TOKEN)) match {
      case Some(subjectToken) =>
        validateSubjectToken(subjectToken) match {
          case Success(tokenObject: AuthenticateResponse) =>
            headerManager.putHeader(KeystoneV3Headers.X_AUTHORIZATION.toString, KeystoneV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified (not in-scope)
            tokenObject.user.id.map { id =>
              headerManager.putHeader(KeystoneV3Headers.X_USER_ID.toString, id)
              headerManager.appendHeader(PowerApiHeader.USER.toString, id, 1.0)
            }
            tokenObject.user.name.map(headerManager.putHeader(KeystoneV3Headers.X_USER_NAME.toString, _))
            tokenObject.project.map { project =>
              project.id.map(headerManager.putHeader(KeystoneV3Headers.X_PROJECT_ID.toString, _))
              project.name.map(headerManager.putHeader(KeystoneV3Headers.X_PROJECT_NAME.toString, _))
            }
            tokenObject.roles.map { roles =>
              headerManager.putHeader(KeystoneV3Headers.X_ROLES, roles.map(_.name) mkString ",")
            }
            headerManager.putHeader(KeystoneV3Headers.X_TOKEN_EXPIRES, tokenObject.expires_at)
            // TODO: Set X-Impersonator-Name, need to check response for impersonator (is this an extension?)
            // TODO: Set X-Impersonator-Id, same as above
            // TODO: Set X-Catalog, need to base64 encode
            // TODO: Set X-Default-Region, may require another API call? Doesn't seem to be returned in a token
            if (forwardUnauthorizedRequests) headerManager.putHeader(KeystoneV3Headers.X_IDENTITY_STATUS, IdentityStatus.Confirmed.name)
            // TODO: Set X-PP-Groups, requires an API call to groups

            filterDirector.setFilterAction(FilterAction.PASS)
          case Failure(e: InvalidSubjectTokenException) =>
            if (forwardUnauthorizedRequests) {
              headerManager.putHeader(KeystoneV3Headers.X_IDENTITY_STATUS, IdentityStatus.Indeterminate.name)
              headerManager.putHeader(KeystoneV3Headers.X_AUTHORIZATION, KeystoneV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified (not in-scope)
              filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
            } else {
              filterDirector.responseHeaderManager.putHeader(KeystoneV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + keystoneServiceUri)
              filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
            }
          case Failure(e: KeystoneServiceException) =>
            LOG.error("Keystone v3 failure: " + e.getMessage)
          case Failure(e: InvalidAdminCredentialsException) =>
            LOG.error("Keystone v3 failure: " + e.getMessage)
          case _ =>
            LOG.error("Validation of subject token '" + subjectToken + "' failed for an unknown reason")
        }
      case _ =>
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
    }

    filterDirector
  }

  private def validateSubjectToken(subjectToken: String, isRetry: Boolean = false): Try[_] = {
    // TODO: What if this token was invalidated before the TTL is exceeded? Returning bad cached tokens. Configurable caching?
    Option(datastore.get(TOKEN_KEY_PREFIX + subjectToken)) match {
      case Some(cachedSubjectTokenObject) =>
        Success(cachedSubjectTokenObject.asInstanceOf[AuthenticateResponse])
      case None =>
        val fetchedAdminToken = fetchAdminToken(isRetry)

        fetchedAdminToken match {
          case Success(adminToken) =>
            val headerMap = Map(
              KeystoneV3Headers.X_AUTH_TOKEN -> adminToken,
              KeystoneV3Headers.X_SUBJECT_TOKEN -> subjectToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            )
            val validateTokenResponse = akkaServiceClient.get(TOKEN_KEY_PREFIX + subjectToken, keystoneServiceUri + KeystoneV3Endpoints.TOKEN, headerMap.asJava)

            HttpStatusCode.fromInt(validateTokenResponse.getStatusCode) match {
              case HttpStatusCode.OK =>
                val subjectTokenObject = readToAuthResponseObject(validateTokenResponse).token

                val expiration = new DateTime(subjectTokenObject.expires_at)
                val identityTtl = safeLongToInt(expiration.getMillis - DateTime.now.getMillis)
                val offsetConfiguredTtl = offsetTtl(tokenCacheTtl, cacheOffset)
                val ttl = if (offsetConfiguredTtl < 1) identityTtl else math.min(offsetConfiguredTtl, identityTtl)
                LOG.debug("Caching token '" + subjectToken + "' with TTL set to: " + ttl + "ms")
                datastore.put(TOKEN_KEY_PREFIX + subjectToken, subjectTokenObject, ttl, TimeUnit.MILLISECONDS)

                Success(subjectTokenObject)
              case HttpStatusCode.NOT_FOUND =>
                LOG.error("Subject token validation failed. Response Code: 404")
                Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
              case HttpStatusCode.UNAUTHORIZED =>
                if (!isRetry) {
                  LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                  validateSubjectToken(subjectToken, isRetry = true)
                } else {
                  LOG.error("Retry after fetching a new admin token failed. Aborting subject token validation for: '" + subjectToken + "'")
                  Failure(new KeystoneServiceException("Valid admin token could not be fetched"))
                }
              case _ =>
                LOG.error("Keystone service returned an unexpected response status code. Response Code: " + validateTokenResponse.getStatusCode)
                Failure(new KeystoneServiceException("Failed to validate subject token"))
            }
          case Failure(e) => fetchedAdminToken
        }
    }
  }

  private def fetchAdminToken(forceFetchAdminToken: Boolean = false) = {
    val createAdminAuthRequest = () => {
      val username = keystoneConfig.getKeystoneService.getUsername
      val password = keystoneConfig.getKeystoneService.getPassword

      val projectScope = Option(keystoneConfig.getKeystoneService.getProjectId) match {
        case Some(projectId) => Some(Scope(project = Some(ProjectScope(id = projectId))))
        case _ => None
      }

      AuthRequestRoot(
        AuthRequest(
          AuthIdentityRequest(
            methods = List("password"),
            password = Some(PasswordCredentials(
              UserNamePasswordRequest(
                name = Some(username),
                password = password
              )
            ))
          ),
          scope = projectScope
        )
      ).toJson.compactPrint
    }

    // Check the cached adminToken. If present, return it. Validity of the token is handled in validateSubjectToken by way of retry.
    if (cachedAdminToken != null && !forceFetchAdminToken) {
      Success(cachedAdminToken)
    } else {
      val generateAuthTokenResponse = akkaServiceClient.post(ADMIN_TOKEN_KEY, keystoneServiceUri + KeystoneV3Endpoints.TOKEN, Map[String, String]().asJava, createAdminAuthRequest(), MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)
      HttpStatusCode.fromInt(generateAuthTokenResponse.getStatusCode) match {
        // Since the operation is a POST, a 201 should be returned if the operation was successful
        case HttpStatusCode.CREATED =>
          val newAdminToken = generateAuthTokenResponse.getHeaders.filter((header: Header) => header.getName.equalsIgnoreCase(KeystoneV3Headers.X_SUBJECT_TOKEN)).head.getValue

          LOG.debug("Caching admin token")
          cachedAdminToken = newAdminToken

          Success(newAdminToken)
        case _ =>
          LOG.error("Unable to get admin token. Please verify your admin credentials. Response Code: " + generateAuthTokenResponse.getStatusCode)
          Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
      }
    }
  }

  private def readToAuthResponseObject(response: ServiceClientResponse) = {
    response.getData.reset() // TODO: Remove this when we can. It relies on our implementation returning an InputStream that supports reset.
    val responseJson = Source.fromInputStream(response.getData).mkString
    responseJson.parseJson.convertTo[AuthResponse]
  }

  private def writeProjectHeader(projectFromUri: String, roles: List[Role], writeAll: Boolean, filterDirector: FilterDirector) = {
    val projectsFromRoles: Set[String] =  if (writeAll) roles.map({ role => role.project_id.get}).toSet else Set.empty
    def projects: Set[String] = projectsFromRoles + projectFromUri

    filterDirector.requestHeaderManager().appendHeader("X-PROJECT-ID", projects.toArray: _*)
  }

  private def containsEndpoint(endpoints: List[Endpoint]): Boolean = endpoints.exists { endpoint: Endpoint =>
    var returnValue = true
    if (endpoint.url != keystoneConfig.getServiceEndpoint.getUrl)
      returnValue = false
    if ((keystoneConfig.getServiceEndpoint.getRegion != null) && !endpoint.region.exists(_ == keystoneConfig.getServiceEndpoint.getRegion))
      returnValue = false
    if ((keystoneConfig.getServiceEndpoint.getName != null) && (keystoneConfig.getServiceEndpoint.getName != endpoint.name))
      returnValue = false
    if ((keystoneConfig.getServiceEndpoint.getInterface != null) && !endpoint.interface.exists(_ == keystoneConfig.getServiceEndpoint.getInterface))
      returnValue = false
    returnValue
  }

  private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[Role]): Boolean = true

  private def matchesProject(projectFromUri: String, roles: List[Role]): Boolean = true

  private val isUriWhitelisted = (requestUri: String, whiteList: List[String]) =>
    whiteList.filter(requestUri.matches).nonEmpty

  private val safeLongToInt = (l: Long) =>
    math.min(l, Int.MaxValue).toInt

  private[v3] val offsetTtl = (exactTtl: Int, offset: Int) =>
    if (offset == 0 || exactTtl == 0) exactTtl
    else safeLongToInt(exactTtl.toLong + (Random.nextInt(offset * 2) - offset))
}
