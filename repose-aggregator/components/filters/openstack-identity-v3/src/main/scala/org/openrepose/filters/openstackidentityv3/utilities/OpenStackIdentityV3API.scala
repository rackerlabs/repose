package org.openrepose.filters.openstackidentityv3.utilities

import java.io.{InputStream, Serializable}
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.Header
import org.joda.time.DateTime
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config
import org.openrepose.filters.openstackidentityv3.json.spray.IdentityJsonProtocol._
import org.openrepose.filters.openstackidentityv3.objects._
import org.openrepose.services.datastore.Datastore
import org.openrepose.services.serviceclient.akka.AkkaServiceClient
import spray.json._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Random, Success, Try}

class OpenStackIdentityV3API(config: OpenstackIdentityV3Config, datastore: Datastore, akkaServiceClient: AkkaServiceClient)
  extends LazyLogging {

  private final val TOKEN_ENDPOINT = "/v3/auth/tokens"
  private final val GROUPS_ENDPOINT = (userId: String) => s"/v3/users/$userId/groups"

  private final val ADMIN_TOKEN_KEY = "IDENTITY:V3:ADMIN_TOKEN"
  private final val TOKEN_KEY_PREFIX = "IDENTITY:V3:TOKEN:"
  private final val GROUPS_KEY_PREFIX = "IDENTITY:V3:GROUPS:"

  private val identityServiceUri = config.getOpenstackIdentityService.getUri
  private val cacheOffset = config.getCacheOffset
  private val tokenCacheTtl = config.getTokenCacheTimeout
  private val groupsCacheTtl = config.getGroupsCacheTimeout

  def getAdminToken(checkCache: Boolean = true): Try[String] = {
    def createAdminAuthRequest() = {
      val username = config.getOpenstackIdentityService.getUsername
      val password = config.getOpenstackIdentityService.getPassword

      val projectScope = Option(config.getOpenstackIdentityService.getProjectId) match {
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

    getFromCache[String](ADMIN_TOKEN_KEY) match {
      case Some(adminToken) if checkCache =>
        Success(adminToken)
      case _ =>
        val authTokenResponse = Option(akkaServiceClient.post(
          ADMIN_TOKEN_KEY,
          identityServiceUri + TOKEN_ENDPOINT,
          Map(CommonHttpHeader.ACCEPT.toString -> MediaType.APPLICATION_JSON).asJava,
          createAdminAuthRequest(),
          MediaType.APPLICATION_JSON_TYPE
        ))

        // Since we *might* get a null back from the akka service client, we have to map it, and then match
        // because we care to match on the status code of the response, if anything was set.
        authTokenResponse map (response => response.getStatus) match {
          case Some(statusCode) if statusCode == HttpServletResponse.SC_CREATED =>
            val newAdminToken = Option(authTokenResponse.get.getHeaders).map(_.filter((header: Header) => header.getName.equalsIgnoreCase(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN)).head.getValue)

            newAdminToken match {
              case Some(token) =>
                logger.debug("Caching admin token")

                val adminTokenObject = jsonStringToObject[AuthResponse](inputStreamToString(authTokenResponse.get.getData)).token
                val adminTokenTtl = safeLongToInt(new DateTime(adminTokenObject.expires_at).getMillis - DateTime.now.getMillis)

                datastore.put(ADMIN_TOKEN_KEY, token, adminTokenTtl, TimeUnit.MILLISECONDS)
                Success(token)
              case None =>
                logger.error("Headers not found in a successful response to an admin token request. The OpenStack Identity service is not adhering to the v3 contract.")
                Failure(new IdentityServiceException("OpenStack Identity service did not return headers with a successful response"))
            }
          case Some(statusCode) =>
            logger.error("Unable to get admin token. Please verify your admin credentials. Response Code: " + statusCode)
            Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
          case None =>
            logger.error("Unable to get admin token. Request to OpenStack Identity service timed out.")
            Failure(new IdentityServiceException("OpenStack Identity service could not be reached to obtain admin token"))
        }
    }
  }

  def validateToken(subjectToken: String, checkCache: Boolean = true): Try[AuthenticateResponse] = {
    getFromCache[AuthenticateResponse](TOKEN_KEY_PREFIX + subjectToken) match {
      case Some(cachedSubjectTokenObject) =>
        Success(cachedSubjectTokenObject)
      case None =>
        getAdminToken(checkCache) match {
          case Success(adminToken) =>
            val headerMap = Map(
              OpenStackIdentityV3Headers.X_AUTH_TOKEN -> adminToken,
              OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> subjectToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            )
            val validateTokenResponse = Option(akkaServiceClient.get(
              TOKEN_KEY_PREFIX + subjectToken,
              identityServiceUri + TOKEN_ENDPOINT,
              headerMap.asJava
            ))

            // Since we *might* get a null back from the akka service client, we have to map it, and then match
            // because we care to match on the status code of the response, if anything was set.
            validateTokenResponse.map(response => response.getStatus) match {
              case Some(statusCode) if statusCode == HttpServletResponse.SC_OK =>
                val subjectTokenObject = jsonStringToObject[AuthResponse](inputStreamToString(validateTokenResponse.get.getData)).token

                val expiration = new DateTime(subjectTokenObject.expires_at)
                val identityTtl = safeLongToInt(expiration.getMillis - DateTime.now.getMillis)
                val offsetConfiguredTtl = offsetTtl(tokenCacheTtl, cacheOffset)
                // TODO: Come up with a better algorithm to decide the cache TTL and handle negative/0 TTLs
                val ttl = if (offsetConfiguredTtl < 1) identityTtl else math.max(math.min(offsetConfiguredTtl, identityTtl), 1)
                logger.debug("Caching token '" + subjectToken + "' with TTL set to: " + ttl + "ms")
                datastore.put(TOKEN_KEY_PREFIX + subjectToken, subjectTokenObject, ttl, TimeUnit.MILLISECONDS)

                Success(subjectTokenObject)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_NOT_FOUND =>
                logger.error("Subject token validation failed. Response Code: 404")
                Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && checkCache =>
                logger.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                validateToken(subjectToken, checkCache = false)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && !checkCache =>
                logger.error("Retry after fetching a new admin token failed. Aborting subject token validation for: '" + subjectToken + "'")
                Failure(new IdentityServiceException("Valid admin token could not be fetched"))
              case Some(_) =>
                logger.error("OpenStack Identity service returned an unexpected response status code. Response Code: " + validateTokenResponse.get.getStatus)
                Failure(new IdentityServiceException("Failed to validate subject token"))
              case None =>
                logger.error("Unable to validate subject token. Request to OpenStack Identity service timed out.")
                Failure(new IdentityServiceException("OpenStack Identity service could not be reached to validate subject token"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  def getGroups(userId: String, checkCache: Boolean = true): Try[List[Group]] = {
    getFromCache[mutable.ArrayBuffer[Group]](GROUPS_KEY_PREFIX + userId) match {
      case Some(cachedGroups) =>
        Success(cachedGroups.toList)
      case None =>
        getAdminToken(checkCache) match {
          case Success(adminToken) =>
            val headerMap = Map(
              OpenStackIdentityV3Headers.X_AUTH_TOKEN -> adminToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            )
            val groupsResponse = Option(akkaServiceClient.get(
              GROUPS_KEY_PREFIX + userId,
              identityServiceUri + GROUPS_ENDPOINT(userId),
              headerMap.asJava
            ))

            // Since we *might* get a null back from the akka service client, we have to map it, and then match
            // because we care to match on the status code of the response, if anything was set.
            groupsResponse.map(response => response.getStatus) match {
              case Some(statusCode) if statusCode == HttpServletResponse.SC_OK =>
                val groups = jsonStringToObject[Groups](inputStreamToString(groupsResponse.get.getData)).groups

                val offsetConfiguredTtl = offsetTtl(groupsCacheTtl, cacheOffset)
                val ttl = if (offsetConfiguredTtl < 1) {
                  logger.error("Offset group cache ttl was negative, defaulting to 10 minutes. Please check your configuration.")
                  600000
                } else {
                  offsetConfiguredTtl
                }
                logger.debug("Caching groups for user '" + userId + "' with TTL set to: " + ttl + "ms")
                // TODO: Maybe handle all this conversion jank?
                datastore.put(GROUPS_KEY_PREFIX + userId, groups.toBuffer.asInstanceOf[Serializable], ttl, TimeUnit.MILLISECONDS)

                Success(groups)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_NOT_FOUND =>
                logger.error("Groups for '" + userId + "' not found. Response Code: 404")
                Failure(new InvalidUserForGroupsException("Failed to fetch groups"))
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && checkCache =>
                logger.error("Request made with an expired admin token. Fetching a fresh admin token and retrying groups retrieval. Response Code: 401")
                getGroups(userId, checkCache = false)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && !checkCache =>
                logger.error("Retry after fetching a new admin token failed. Aborting groups retrieval for: '" + userId + "'")
                Failure(new IdentityServiceException("Valid admin token could not be fetched"))
              case Some(statusCode) =>
                logger.error("OpenStack Identity service returned an unexpected response status code. Response Code: " + statusCode)
                Failure(new IdentityServiceException("Failed to fetch groups"))
              case None =>
                logger.error("Unable to get groups. Request to OpenStack Identity service timed out.")
                Failure(new IdentityServiceException("OpenStack Identity service could not be reached to obtain groups"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  private def getFromCache[T <: Serializable](key: String): Option[T] = {
    try {
      Option(datastore.get(key)).map(_.asInstanceOf[T])
    } catch {
      case cce: ClassCastException => None
    }
  }

  private def inputStreamToString(inputStream: InputStream) = {
    inputStream.reset() // TODO: Remove this when we can. It relies on our implementation returning an InputStream that supports reset.
    Source.fromInputStream(inputStream).mkString
  }

  private def jsonStringToObject[T: JsonFormat](json: String) =
    json.parseJson.convertTo[T]

  private def safeLongToInt(l: Long) =
    math.min(l, Int.MaxValue).toInt

  private def offsetTtl(exactTtl: Int, offset: Int) = {
    if (offset == 0 || exactTtl == 0) exactTtl
    else safeLongToInt(exactTtl.toLong + (Random.nextInt(offset * 2) - offset))
  }
}
