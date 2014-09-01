package com.rackspace.papi.components.keystone.v3

import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.rackspace.papi.commons.util.http._
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.keystone.v3.config.{WhiteList, KeystoneV3Config}
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
    if (isUriWhitelisted(request.getRequestURI, keystoneConfig.getWhiteList)) {
      LOG.debug("Request URI matches a configured whitelist pattern! Allowing request to pass through.")

      val filterDirector: FilterDirector = new FilterDirectorImpl()
      filterDirector.setFilterAction(FilterAction.PASS)
      filterDirector
    } else {
      val (filterDirector, authenticateResponse) = authenticate(request)
      if (!validateEndpoint(authenticateResponse)) {
        //Endpoint does not validate, or was required, but not returned from identity
        filterDirector.setFilterAction(FilterAction.RETURN)
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
      }
      filterDirector
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

  /**
   * Taking an AuthenticateResponse, we can get all the headers we might possibly set, and then remove the Nones
   * from it. Reads a bit better and is much easier to verify what headers are generated based on the
   * AuthenticateResponse
   * @param tokenObject
   * @return
   */
  def headersToSet(tokenObject: AuthenticateResponse): Map[String, String] = {
    import KeystoneV3Headers._

    // TODO: Add the project ID if verified (not in-scope)
    val rootHeaders: Map[String, Option[String]] = Map(
      X_TOKEN_EXPIRES -> Some(tokenObject.expires_at),
      X_AUTHORIZATION -> Some(X_AUTH_PROXY),
      X_USER_NAME -> tokenObject.user.name,
      PowerApiHeader.X_CATALOG.toString -> tokenObject.catalog.map(catalog => base64Encode(catalog.toJson.compactPrint)),
      X_ROLES -> tokenObject.roles.map(roles => roles.map(_.name) mkString ","),
      X_USER_ID -> tokenObject.user.id,
      X_IDENTITY_STATUS -> (if (forwardUnauthorizedRequests) Some(IdentityStatus.Confirmed.name) else None)
    )

    val projectHeaders: Map[String, Option[String]] = tokenObject.project.map {
      project =>
        Map(
          X_PROJECT_ID -> project.id,
          X_PROJECT_NAME -> project.name
        )
    }.getOrElse(Map.empty[String, Option[String]])

    // TODO: Set X-Impersonator-Name, need to check response for impersonator (out of scope)
    // TODO: Set X-Impersonator-Id, same as above
    // TODO: Set X-Default-Region, may require another API call? Doesn't seem to be returned in a token

    //Strip out any optionals from our headers, so we just get the headers we have values for
    (rootHeaders ++ projectHeaders).collect {
      case (key, Some(value)) => key -> value
    }
  }

  /**
   * Just a convenience method to append a quality to a header. It's nested in the header manager, which is annoying
   * to use, because it's mutable.
   * @param value
   * @param quality
   * @return
   */
  def headerValueWithQuality(value: String, quality: Double) = value + ";q=" + quality.toString

  /**
   * Build a map of headers from a tokenObject, and a list of Groups. This is somewhat easier to read, and much
   * easier to test.
   * @param tokenObject
   * @param groups
   * @return
   */
  def headersToAppend(tokenObject: AuthenticateResponse, groups: List[Group]): Map[String, List[String]] = {

    val projectOptionalHeaders = tokenObject.project.map {
      project =>
        Map(
          PowerApiHeader.USER.toString -> project.name.map(name => List(headerValueWithQuality(name, 1.0)))
        )
    } getOrElse {
      Map.empty[String, Option[List[String]]]
    }

    val groupsList: Option[List[String]] = if (groups.nonEmpty) {
      Some(groups.map { group =>
        headerValueWithQuality(group.name, 1.0)
      })
    } else {
      None
    }

    //Collect only the ones with possible values, and return them
    (projectOptionalHeaders + (PowerApiHeader.GROUPS.toString -> groupsList)).collect {
      case (key, Some(value)) => key -> value
    }
  }

  // TODO: Drop the tuple, return an AuthenticateResponse, and handle the filter director a level up
  private def authenticate(request: HttpServletRequest): (FilterDirector, Option[AuthenticateResponse]) = {
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()
    filterDirector.setFilterAction(FilterAction.RETURN)
    filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR)

    //blah this is super gross, but it works...
    //TODO restructure this jank to avoid the var, it can be done!
    var authenticateResponse: AuthenticateResponse = null
    Option(request.getHeader(KeystoneV3Headers.X_SUBJECT_TOKEN)) match {
      case Some(subjectToken) =>
        validateSubjectToken(subjectToken) match {
          case Success(tokenObject: AuthenticateResponse) =>
            authenticateResponse = tokenObject

            //Collect the headers we're going to set
            val putHeaders = headersToSet(tokenObject)

            //Get the list of groups if configured to, otherwise just an empty group list
            val groupList = if (keystoneConfig.isRequestGroups) {
              tokenObject.user.id.map { userId =>
                fetchGroups(userId).getOrElse(List.empty[Group])
              } getOrElse {
                LOG.warn("The X-PP-Groups header could not be populated. The user ID was not present in the token retrieved from Keystone.")
                List.empty[Group]
              }
            } else {
              List.empty[Group]
            }

            //Get the list of headers that we have to call "append" on
            val appendHeaders = headersToAppend(tokenObject, groupList)

            //Put all the headers
            putHeaders.foreach { case (key, value) =>
              headerManager.putHeader(key, value)
            }

            //Append all the headers
            appendHeaders.foreach { case (key, value) =>
              //Use the magic list expansion to get to a java String... varargs
              headerManager.appendHeader(key, value: _*)
            }

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
      case None =>
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED)
    }

    (filterDirector, Option(authenticateResponse))
  }

  /**
   * Takes an auth response and determines if their endpoint is valid
   * @param authenticateResponse Optional
   * @return true if they are authorized, false otherwise
   */
  def validateEndpoint(authenticateResponse: Option[AuthenticateResponse]): Boolean = {
    authenticateResponse match {
      case Some(authResponse) =>
        (Option(keystoneConfig.getServiceEndpoint), authResponse.catalog) match {
          //If I have both a required endpoint config and a catalog
          // then I go verify that the required endpoint is in my list
          case (Some(endpoint), Some(catalog)) =>
            val endpoints = catalog.service.flatMap(service => service.endpoints)
            containsEndpoint(endpoints)
          case (Some(_), None) => false
          case (None, _) => true
        }
      case None => false
    }
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
            val validateTokenResponse = akkaServiceClient.get(TOKEN_KEY_PREFIX + subjectToken, keystoneServiceUri + KeystoneV3Endpoints.TOKEN, headerMap.asJava)

            HttpStatusCode.fromInt(validateTokenResponse.getStatusCode) match {
              case HttpStatusCode.OK =>
                val subjectTokenObject = jsonStringToObject[AuthResponse](inputStreamToString(validateTokenResponse.getData)).token

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
              case HttpStatusCode.UNAUTHORIZED if !isRetry =>
                LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                validateSubjectToken(subjectToken, isRetry = true)
              case HttpStatusCode.UNAUTHORIZED if isRetry =>
                LOG.error("Retry after fetching a new admin token failed. Aborting subject token validation for: '" + subjectToken + "'")
                Failure(new KeystoneServiceException("Valid admin token could not be fetched"))
              case _ =>
                LOG.error("Keystone service returned an unexpected response status code. Response Code: " + validateTokenResponse.getStatusCode)
                Failure(new KeystoneServiceException("Failed to validate subject token"))
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
            val groupsResponse = akkaServiceClient.get(GROUPS_KEY_PREFIX + userId, keystoneServiceUri + KeystoneV3Endpoints.GROUPS(userId), headerMap.asJava)

            HttpStatusCode.fromInt(groupsResponse.getStatusCode) match {
              case HttpStatusCode.OK =>
                val groups = jsonStringToObject[Groups](inputStreamToString(groupsResponse.getData)).groups

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
              case HttpStatusCode.NOT_FOUND =>
                LOG.error("Groups for '" + userId + "' not found. Response Code: 404")
                Failure(new InvalidUserForGroupsException("Failed to fetch groups"))
              case HttpStatusCode.UNAUTHORIZED =>
                if (!isRetry) {
                  LOG.error("Request made with an expired admin token. Fetching a fresh admin token and retrying groups retrieval. Response Code: 401")
                  fetchGroups(userId, isRetry = true)
                } else {
                  LOG.error("Retry after fetching a new admin token failed. Aborting groups retrieval for: '" + userId + "'")
                  Failure(new KeystoneServiceException("Valid admin token could not be fetched"))
                }
              case _ =>
                LOG.error("Keystone service returned an unexpected response status code. Response Code: " + groupsResponse.getStatusCode)
                Failure(new KeystoneServiceException("Failed to fetch groups"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  private def writeProjectHeader(projectFromUri: String, roles: List[Role], writeAll: Boolean, filterDirector: FilterDirector) = {
    val projectsFromRoles: Set[String] = if (writeAll) roles.map({
      role => role.project_id.get
    }).toSet
    else Set.empty
    def projects: Set[String] = projectsFromRoles + projectFromUri

    filterDirector.requestHeaderManager().appendHeader("X-PROJECT-ID", projects.toArray: _*)
  }

  /**
   * Using a method on the endpoint itself hiding a pattern match to determine if it matches.
   * see the matches method on the Endpoint caseClass.
   * @param endpoints
   * @return
   */
  def containsEndpoint(endpoints: List[Endpoint]): Boolean = {
    endpoints.exists {
      endpoint =>
        endpoint.matches(requiredUrl = keystoneConfig.getServiceEndpoint.getUrl,
          requiredRegion = Option(keystoneConfig.getServiceEndpoint.getRegion),
          requiredName = Option(keystoneConfig.getServiceEndpoint.getName),
          requiredInterface = Option(keystoneConfig.getServiceEndpoint.getInterface)
        )
    }
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

  private val isUriWhitelisted = (requestUri: String, whiteList: WhiteList) => {
    val convertedWhitelist = Option(whiteList).map(_.getUriPattern.asScala.toList).getOrElse(List.empty[String])
    convertedWhitelist.filter(requestUri.matches).nonEmpty
  }

  private val safeLongToInt = (l: Long) =>
    math.min(l, Int.MaxValue).toInt

  private[v3] val offsetTtl = (exactTtl: Int, offset: Int) =>
    if (offset == 0 || exactTtl == 0) exactTtl
    else safeLongToInt(exactTtl.toLong + (Random.nextInt(offset * 2) - offset))
}
