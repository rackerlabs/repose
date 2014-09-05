package com.rackspace.papi.components.keystone.v3

import java.io.InputStream
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
import org.apache.commons.codec.binary.Base64
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
  private final val GROUPS_KEY_PREFIX = "GROUPS:"

  private lazy val keystoneServiceUri = keystoneConfig.getKeystoneService.getUri
  private lazy val tokenCacheTtl = keystoneConfig.getTokenCacheTimeout
  private lazy val groupsCacheTtl = keystoneConfig.getGroupsCacheTimeout
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
      authorize(authenticate(request))
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
      case _ =>
        LOG.trace("Response from origin service requires no additional processing. Passing it along.")
    }

    LOG.debug("Keystone v3 Handling Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }

  // TODO: Drop the tuple, return an AuthenticateResponse, and handle the filter director a level up
  private def authenticate(request: HttpServletRequest): (FilterDirector, AuthenticateResponse) = {
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()
    filterDirector.setFilterAction(FilterAction.RETURN)
    filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)

    var authSuccess = false
    var authenticateResponse: AuthenticateResponse = null
    Option(request.getHeader(KeystoneV3Headers.X_SUBJECT_TOKEN)) match {
      case Some(subjectToken) =>
        validateSubjectToken(subjectToken) match {
          case Success(tokenObject: AuthenticateResponse) =>
            authSuccess = true
            authenticateResponse = tokenObject

            headerManager.putHeader(KeystoneV3Headers.X_TOKEN_EXPIRES, tokenObject.expires_at)
            headerManager.putHeader(KeystoneV3Headers.X_AUTHORIZATION.toString, KeystoneV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified (not in-scope)
            tokenObject.user.name.map(headerManager.putHeader(KeystoneV3Headers.X_USER_NAME.toString, _))
            tokenObject.catalog.map(catalog => headerManager.putHeader(PowerApiHeader.X_CATALOG.toString, base64Encode(catalog.toJson.compactPrint)))
            tokenObject.roles.map { roles =>
              headerManager.putHeader(KeystoneV3Headers.X_ROLES, roles.map(_.name) mkString ",")
            }
            tokenObject.user.id.map { id =>
              headerManager.putHeader(KeystoneV3Headers.X_USER_ID.toString, id)
              headerManager.appendHeader(PowerApiHeader.USER.toString, id, 1.0)
            }
            tokenObject.project.map { project =>
              project.id.map(headerManager.putHeader(KeystoneV3Headers.X_PROJECT_ID.toString, _))
              project.name.map(headerManager.putHeader(KeystoneV3Headers.X_PROJECT_NAME.toString, _))
            }
            if (keystoneConfig.isRequestGroups) {
              tokenObject.user.id map { userId: String =>
                fetchGroups(userId) map { groupsList: List[Group] =>
                  groupsList map { group: Group =>
                    headerManager.appendHeader(PowerApiHeader.GROUPS.toString, group.name + ";q=1.0")
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

            filterDirector.setFilterAction(FilterAction.PASS)
          case Failure(e: InvalidSubjectTokenException) =>
            filterDirector.responseHeaderManager.putHeader(KeystoneV3Headers.WWW_AUTHENTICATE, "Keystone uri=" + keystoneServiceUri)
            filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
          case Failure(e: KeystoneServiceException) =>
            LOG.error("Keystone v3 failure: " + e.getMessage)
          case Failure(e: InvalidAdminCredentialsException) =>
            LOG.error("Keystone v3 failure: " + e.getMessage)
          case _ =>
            LOG.error("Validation of subject token '" + subjectToken + "' failed for an unknown reason")
        }
      case None =>
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
    }

    if (forwardUnauthorizedRequests) {
      if (authSuccess) {
        headerManager.putHeader(KeystoneV3Headers.X_IDENTITY_STATUS, IdentityStatus.Confirmed.name)
      } else {
        LOG.debug("Forwarding indeterminate request") // TODO: Should this be info or debug?
        headerManager.putHeader(KeystoneV3Headers.X_IDENTITY_STATUS, IdentityStatus.Indeterminate.name)
        headerManager.putHeader(KeystoneV3Headers.X_AUTHORIZATION, KeystoneV3Headers.X_AUTH_PROXY) // TODO: Add the project ID if verified (not in-scope)
        filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
      }
    }

    (filterDirector, authenticateResponse)
  }

  // TODO: Extract the filter director, take a list of ServiceForAuthenticateResponse, return a boolean
  private def authorize(tuple: (FilterDirector, AuthenticateResponse)): FilterDirector = {
    val filterDirector = tuple._1
    val authResponse = tuple._2
    if (Option(keystoneConfig.getServiceEndpoint).isDefined && !containsEndpoint(authResponse.catalog.map(catalog => catalog.map(service => service.endpoints).flatten).getOrElse(List.empty[Endpoint]))) {
      filterDirector.setFilterAction(FilterAction.RETURN)
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
    }

    filterDirector
  }

  private def validateSubjectToken(subjectToken: String, isRetry: Boolean = false): Try[AuthenticateResponse] = {
    Option(datastore.get(TOKEN_KEY_PREFIX + subjectToken)) match {
      case Some(cachedSubjectTokenObject) =>
        Success(cachedSubjectTokenObject.asInstanceOf[AuthenticateResponse])
      case None =>
        fetchAdminToken(isRetry) match {
          case Success(adminToken) =>
            val headerMap = Map(
              KeystoneV3Headers.X_AUTH_TOKEN -> adminToken,
              KeystoneV3Headers.X_SUBJECT_TOKEN -> subjectToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            )
            val validateTokenResponse = Option(akkaServiceClient.get(TOKEN_KEY_PREFIX + subjectToken,
              keystoneServiceUri + KeystoneV3Endpoints.TOKEN,
              headerMap.asJava))

            // Since we *might* get a null back from the akka service client, we have to map it, and then match
            // because we care to match on the status code of the response, if anything was set.
            validateTokenResponse.map(response => HttpStatusCode.fromInt(response.getStatusCode)) match {
              case Some(statusCode) if statusCode == HttpStatusCode.OK =>
                val subjectTokenObject = jsonStringToObject[AuthResponse](inputStreamToString(validateTokenResponse.get.getData)).token

                val expiration = new DateTime(subjectTokenObject.expires_at)
                val identityTtl = safeLongToInt(expiration.getMillis - DateTime.now.getMillis)
                val offsetConfiguredTtl = offsetTtl(tokenCacheTtl, cacheOffset)
                // TODO: Come up with a better algorithm to decide the cache TTL and handle negative/0 TTLs
                val ttl = if (offsetConfiguredTtl < 1) identityTtl else math.max(math.min(offsetConfiguredTtl, identityTtl), 1)
                LOG.debug("Caching token '" + subjectToken + "' with TTL set to: " + ttl + "ms")
                datastore.put(TOKEN_KEY_PREFIX + subjectToken, subjectTokenObject, ttl, TimeUnit.MILLISECONDS)

                Success(subjectTokenObject)
              case Some(statusCode) if statusCode == HttpStatusCode.NOT_FOUND =>
                LOG.error("Subject token validation failed. Response Code: 404")
                Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
              case Some(statusCode) if statusCode == HttpStatusCode.UNAUTHORIZED =>
                if (!isRetry) {
                  LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                  validateSubjectToken(subjectToken, isRetry = true)
                } else {
                  LOG.error("Retry after fetching a new admin token failed. Aborting subject token validation for: '" + subjectToken + "'")
                  Failure(new KeystoneServiceException("Valid admin token could not be fetched"))
                }
              case Some(_) =>
                LOG.error("Keystone service returned an unexpected response status code. Response Code: " + validateTokenResponse.get.getStatusCode)
                Failure(new KeystoneServiceException("Failed to validate subject token"))
              case None =>
                LOG.error("Unable to validate subject token. Request to Keystone service timed out.")
                Failure(new KeystoneServiceException("Keystone service could not be reached to validate subject token"))
            }
          case Failure(e) => Failure(e)
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
    if ((cachedAdminToken != null) && !forceFetchAdminToken) {
      Success(cachedAdminToken)
    } else {
      val authTokenResponse = Option(akkaServiceClient.post(ADMIN_TOKEN_KEY,
        keystoneServiceUri + KeystoneV3Endpoints.TOKEN,
        Map[String, String]().asJava,
        createAdminAuthRequest(),
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE))

      // Since we *might* get a null back from the akka service client, we have to map it, and then match
      // because we care to match on the status code of the response, if anything was set.
      authTokenResponse.map(response => HttpStatusCode.fromInt(response.getStatusCode)) match {
        // Since the operation is a POST, a 201 should be returned if the operation was successful
        case Some(statusCode) if statusCode == HttpStatusCode.CREATED =>
          val newAdminToken = Option(authTokenResponse.get.getHeaders).map(_.filter((header: Header) => header.getName.equalsIgnoreCase(KeystoneV3Headers.X_SUBJECT_TOKEN)).head.getValue)

          newAdminToken match {
            case Some(token) =>
              LOG.debug("Caching admin token")
              cachedAdminToken = token
              Success(token)
            case None =>
              LOG.error("Headers not found in a successful response to an admin token request. The Keystone service is not adhering to the Keystone v3 contract.")
              Failure(new KeystoneServiceException("Keystone service did not return headers with a successful response"))
          }
        case Some(_) =>
          LOG.error("Unable to get admin token. Please verify your admin credentials. Response Code: " + authTokenResponse.get.getStatusCode)
          Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
        case None =>
          LOG.error("Unable to get admin token. Request to Keystone service timed out.")
          Failure(new KeystoneServiceException("Keystone service could not be reached to obtain admin token"))
      }
    }
  }

  private def fetchGroups(userId: String, isRetry: Boolean = false): Try[List[Group]] = {
    Option(datastore.get(GROUPS_KEY_PREFIX + userId)) match {
      case Some(cachedGroups) =>
        Success(cachedGroups.asInstanceOf[collection.mutable.Buffer[Group]].toList)
      case None =>
        fetchAdminToken(isRetry) match {
          case Success(adminToken) =>
            val headerMap = Map(
              KeystoneV3Headers.X_AUTH_TOKEN -> adminToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            )
            val groupsResponse = Option(akkaServiceClient.get(GROUPS_KEY_PREFIX + userId,
              keystoneServiceUri + KeystoneV3Endpoints.GROUPS(userId),
              headerMap.asJava))

            // Since we *might* get a null back from the akka service client, we have to map it, and then match
            // because we care to match on the status code of the response, if anything was set.
            groupsResponse.map(response => HttpStatusCode.fromInt(response.getStatusCode)) match {
              case Some(statusCode) if statusCode == HttpStatusCode.OK =>
                val groups = jsonStringToObject[Groups](inputStreamToString(groupsResponse.get.getData)).groups

                val offsetConfiguredTtl = offsetTtl(groupsCacheTtl, cacheOffset)
                val ttl = if (offsetConfiguredTtl < 1) {
                  LOG.error("Offset group cache ttl was negative, defaulting to 10 minutes. Please check your configuration.")
                  600000
                } else {
                  offsetConfiguredTtl
                }
                LOG.debug("Caching groups for user '" + userId + "' with TTL set to: " + ttl + "ms")
                // TODO: Maybe handle all this conversion jank?
                datastore.put(GROUPS_KEY_PREFIX + userId, groups.toBuffer.asInstanceOf[Serializable], ttl, TimeUnit.MILLISECONDS)

                Success(groups)
              case Some(statusCode) if statusCode == HttpStatusCode.NOT_FOUND =>
                LOG.error("Groups for '" + userId + "' not found. Response Code: 404")
                Failure(new InvalidUserForGroupsException("Failed to fetch groups"))
              case Some(statusCode) if statusCode == HttpStatusCode.UNAUTHORIZED =>
                if (!isRetry) {
                  LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying groups retrieval. Response Code: 401")
                  fetchGroups(userId, isRetry = true)
                } else {
                  LOG.error("Retry after fetching a new admin token failed. Aborting groups retrieval for: '" + userId + "'")
                  Failure(new KeystoneServiceException("Valid admin token could not be fetched"))
                }
              case Some(_) =>
                LOG.error("Keystone service returned an unexpected response status code. Response Code: " + groupsResponse.get.getStatusCode)
                Failure(new KeystoneServiceException("Failed to fetch groups"))
              case None =>
                LOG.error("Unable to get groups. Request to Keystone service timed out.")
                Failure(new KeystoneServiceException("Keystone service could not be reached to obtain groups"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  private def writeProjectHeader(projectFromUri: String, roles: List[Role], writeAll: Boolean, filterDirector: FilterDirector) = {
    val projectsFromRoles: Set[String] = if (writeAll) roles.map({ role => role.project_id.get}).toSet else Set.empty
    def projects: Set[String] = projectsFromRoles + projectFromUri

    filterDirector.requestHeaderManager().appendHeader("X-PROJECT-ID", projects.toArray: _*)
  }

  private def containsEndpoint(endpoints: List[Endpoint]): Boolean = endpoints.exists { endpoint: Endpoint =>
    (endpoint.url == keystoneConfig.getServiceEndpoint.getUrl) &&
      Option(keystoneConfig.getServiceEndpoint.getRegion).map(region => endpoint.region.exists(_ == region)).getOrElse(true) &&
      Option(keystoneConfig.getServiceEndpoint.getName).map(name => endpoint.name.exists(_ == name)).getOrElse(true) &&
      Option(keystoneConfig.getServiceEndpoint.getInterface).map(interface => endpoint.interface.exists(_ == interface)).getOrElse(true)
  }

  private def hasIgnoreEnabledRole(ignoreProjectRoles: List[String], userRoles: List[Role]): Boolean = true

  private def matchesProject(projectFromUri: String, roles: List[Role]): Boolean = true

  private def base64Encode(s: String) =
    Base64.encodeBase64String(s.getBytes)

  private def inputStreamToString(inputStream: InputStream) = {
    inputStream.reset() // TODO: Remove this when we can. It relies on our implementation returning an InputStream that supports reset.
    Source.fromInputStream(inputStream).mkString
  }

  private def jsonStringToObject[T: JsonFormat](json: String) =
    json.parseJson.convertTo[T]

  private val isUriWhitelisted = (requestUri: String, whiteList: List[String]) =>
    whiteList.filter(requestUri.matches).nonEmpty

  private val safeLongToInt = (l: Long) =>
    math.min(l, Int.MaxValue).toInt

  private[v3] val offsetTtl = (exactTtl: Int, offset: Int) =>
    if (offset == 0 || exactTtl == 0) exactTtl
    else safeLongToInt(exactTtl.toLong + (Random.nextInt(offset * 2) - offset))
}
