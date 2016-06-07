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
package org.openrepose.filters.openstackidentityv3.utilities

import java.io.{InputStream, Serializable}
import java.util.concurrent.TimeUnit
import java.util.{Calendar, GregorianCalendar}
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import com.fasterxml.jackson.core.JsonProcessingException
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.Header
import org.joda.time.DateTime
import org.openrepose.commons.utils.http.{CommonHttpHeader, HttpDate, ServiceClientResponse}
import org.openrepose.core.services.datastore.Datastore
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config
import org.openrepose.filters.openstackidentityv3.json.spray.IdentityJsonProtocol._
import org.openrepose.filters.openstackidentityv3.objects._
import org.springframework.http.HttpHeaders
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Random, Success, Try}

class OpenStackIdentityV3API(config: OpenstackIdentityV3Config, datastore: Datastore, akkaServiceClient: AkkaServiceClient)
  extends LazyLogging {

  private final val SC_TOO_MANY_REQUESTS = 429
  private final val TOKEN_ENDPOINT = "/v3/auth/tokens"
  private final val GROUPS_ENDPOINT = (userId: String) => s"/v3/users/$userId/groups"

  private final val ADMIN_TOKEN_KEY = "IDENTITY:V3:ADMIN_TOKEN"
  private final val TOKEN_KEY_PREFIX = "IDENTITY:V3:TOKEN:"
  private final val GROUPS_KEY_PREFIX = "IDENTITY:V3:GROUPS:"

  private val identityServiceUri = config.getOpenstackIdentityService.getUri
  private val cacheOffset = config.getCacheOffset
  private val tokenCacheTtl = config.getTokenCacheTimeout
  private val groupsCacheTtl = config.getGroupsCacheTimeout

  def getAdminToken(tracingHeader: Option[String] = None, checkCache: Boolean = true): Try[String] = {
    def createAdminAuthRequest() = {
      val username = config.getOpenstackIdentityService.getUsername
      val password = config.getOpenstackIdentityService.getPassword

      // don't include these fields at all if they're going to be null
      val domain = Option(config.getOpenstackIdentityService.getDomainId).map(id => Seq(
        "domain" -> JsObject(Seq(
          "id" -> JsString(id)
        ))
      )) getOrElse Seq()

      val scope = Option(config.getOpenstackIdentityService.getProjectId).map(id => Seq(
        "scope" -> JsObject(Seq(
          "project" -> JsObject(Seq(
            "id" -> JsString(id)
          ))
        ))
      )) getOrElse Seq()

      Json.stringify(JsObject(Seq(
        "auth" -> JsObject(Seq(
          "identity" -> JsObject(Seq(
            "methods" -> JsArray(Seq(JsString("password"))),
            "password" -> JsObject(Seq(
              "user" -> JsObject(
                domain ++ Seq(
                  "name" -> JsString(username),
                  "password" -> JsString(password)
              ))
            ))
          ))
        ) ++ scope)
      )))
    }

    getFromCache[String](ADMIN_TOKEN_KEY) match {
      case Some(adminToken) if checkCache =>
        Success(adminToken)
      case _ =>
        val requestTracingHeader = tracingHeader.map(headerValue => Map(CommonHttpHeader.TRACE_GUID.toString -> headerValue))
          .getOrElse(Map())
        val headerMap = Map(CommonHttpHeader.ACCEPT.toString -> MediaType.APPLICATION_JSON) ++ requestTracingHeader
        val adminReq = createAdminAuthRequest()
        val authTokenResponse = Option(akkaServiceClient.post(
          ADMIN_TOKEN_KEY,
          identityServiceUri + TOKEN_ENDPOINT,
          headerMap.asJava,
          adminReq,
          MediaType.APPLICATION_JSON_TYPE
        ))

        // Since we *might* get a null back from the akka service client, we have to map it, and then match
        // because we care to match on the status code of the response, if anything was set.
        authTokenResponse map (response => response.getStatus) match {
          case Some(statusCode) if statusCode == HttpServletResponse.SC_CREATED =>
            val newAdminToken = Option(authTokenResponse.get.getHeaders).map(_.filter((header: Header) =>
              header.getName.equalsIgnoreCase(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN)).head.getValue)

            newAdminToken match {
              case Some(token) =>
                logger.debug("Caching admin token")

                try {
                  val json = Json.parse(inputStreamToString(authTokenResponse.get.getData))
                  val tokenExpiration = (json \ "token" \ "expires_at").as[String]
                  val adminTokenTtl = safeLongToInt(new DateTime(tokenExpiration).getMillis - DateTime.now.getMillis)

                  datastore.put(ADMIN_TOKEN_KEY, token, adminTokenTtl, TimeUnit.MILLISECONDS)
                  Success(token)
                } catch {
                  case oops@(_: JsResultException | _: JsonProcessingException) =>
                    Failure(new IdentityServiceException("Unable to parse JSON from identity validate token response", oops))
                }
              case None =>
                logger.error("Headers not found in a successful response to an admin token request. The OpenStack Identity service is not adhering to the v3 contract.")
                Failure(new IdentityServiceException("OpenStack Identity service did not return headers with a successful response"))
            }
          case Some(statusCode) if statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE || statusCode == SC_TOO_MANY_REQUESTS =>
            logger.error(s"Unable to get admin token. OpenStack Identity service returned an Over Limit response status code. Response Code: $statusCode")
            Failure(buildIdentityServiceOverLimitException(authTokenResponse.get))
          case Some(statusCode) =>
            logger.error(s"Unable to get admin token. Please verify your admin credentials. Response Code: $statusCode")
            Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
          case None =>
            logger.error("Unable to get admin token. Request to OpenStack Identity service timed out.")
            Failure(new IdentityServiceException("OpenStack Identity service could not be reached to obtain admin token"))
        }
    }
  }

  def validateToken(subjectToken: String, tracingHeader: Option[String] = None, checkCache: Boolean = true): Try[AuthenticateResponse] = {
    getFromCache[AuthenticateResponse](TOKEN_KEY_PREFIX + subjectToken) match {
      case Some(cachedSubjectTokenObject) =>
        Success(cachedSubjectTokenObject)
      case None =>
        getAdminToken(tracingHeader, checkCache) match {
          case Success(adminToken) =>
            val requestTracingHeader = tracingHeader
              .map(headerValue => Map(CommonHttpHeader.TRACE_GUID.toString -> headerValue))
              .getOrElse(Map())
            val headerMap = Map(
              OpenStackIdentityV3Headers.X_AUTH_TOKEN -> adminToken,
              OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> subjectToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            ) ++ requestTracingHeader
            val validateTokenResponse = Option(akkaServiceClient.get(
              TOKEN_KEY_PREFIX + subjectToken,
              identityServiceUri + TOKEN_ENDPOINT,
              headerMap.asJava
            ))

            // Since we *might* get a null back from the akka service client, we have to map it, and then match
            // because we care to match on the status code of the response, if anything was set.
            validateTokenResponse.map(response => response.getStatus) match {
              case Some(statusCode) if statusCode == HttpServletResponse.SC_OK =>
                val json = Json.parse(inputStreamToString(validateTokenResponse.get.getData))

                val tokenExpiration = (json \ "token" \ "expires_at").as[String]
                val project = (json \ "token" \ "project").toOption.map(_ =>
                  ProjectForAuthenticateResponse(
                    (json \ "token" \ "project" \ "id").asOpt[String],
                    (json \ "token" \ "project" \ "name").asOpt[String]))
                val catalog = (json \ "token" \ "catalog").asOpt[JsArray].map(_.value.map(jsCatalog =>
                  ServiceForAuthenticationResponse(
                    (jsCatalog \ "endpoints").as[JsArray].value.map(jsEndpoint =>
                      Endpoint(
                        (jsEndpoint \ "id").as[String],
                        (jsEndpoint \ "name").asOpt[String],
                        (jsEndpoint \ "interface").asOpt[String],
                        (jsEndpoint \ "region").asOpt[String],
                        (jsEndpoint \ "url").as[String],
                        (jsEndpoint \ "service_id").asOpt[String])).toList,
                    (jsCatalog \ "id").asOpt[String],
                    (jsCatalog \ "name").asOpt[String])).toList)
                val roles = (json \ "token" \ "roles").asOpt[JsArray].map(_.value.map(jsRole =>
                  Role(
                    (jsRole \ "name").as[String],
                    (jsRole \ "project_id").asOpt[String],
                    (jsRole \ "RAX-AUTH:project_id").asOpt[String])).toList)
                val user = UserForAuthenticateResponse(
                  (json \ "token" \ "user" \ "id").asOpt[String],
                  (json \ "token" \ "user" \ "name").asOpt[String],
                  (json \ "token" \ "user" \ "RAX-AUTH:defaultRegion").asOpt[String])
                val raxImpersonator = (json \ "token" \ "RAX-AUTH:impersonator").toOption.map(_ =>
                  ImpersonatorForAuthenticationResponse(
                    (json \ "token" \ "RAX-AUTH:impersonator" \ "id").asOpt[String],
                    (json \ "token" \ "RAX-AUTH:impersonator" \ "name").asOpt[String]))
                val subjectTokenObject = AuthenticateResponse(tokenExpiration, project, catalog, roles, user, raxImpersonator)

                val expiration = new DateTime(tokenExpiration)
                val identityTtl = safeLongToInt(expiration.getMillis - DateTime.now.getMillis)
                val offsetConfiguredTtl = offsetTtl(tokenCacheTtl, cacheOffset)
                // TODO: Come up with a better algorithm to decide the cache TTL and handle negative/0 TTLs
                val ttl = if (offsetConfiguredTtl < 1) identityTtl else math.max(math.min(offsetConfiguredTtl, identityTtl), 1)
                logger.debug(s"Caching token '$subjectToken' with TTL set to: ${ttl}ms")
                datastore.put(TOKEN_KEY_PREFIX + subjectToken, subjectTokenObject, ttl, TimeUnit.MILLISECONDS)

                Success(subjectTokenObject)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_NOT_FOUND =>
                logger.error("Subject token validation failed. Response Code: 404")
                Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && checkCache =>
                logger.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                validateToken(subjectToken, tracingHeader, checkCache = false)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && !checkCache =>
                logger.error(s"Retry after fetching a new admin token failed. Aborting subject token validation for: '$subjectToken'")
                Failure(new IdentityServiceException("Valid admin token could not be fetched"))
              case Some(statusCode) if statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE || statusCode == SC_TOO_MANY_REQUESTS =>
                logger.error(s"OpenStack Identity service returned an Over Limit response status code. Response Code: $statusCode")
                Failure(buildIdentityServiceOverLimitException(validateTokenResponse.get))
              case Some(statusCode) =>
                logger.error(s"OpenStack Identity service returned an unexpected response status code. Response Code: $statusCode")
                Failure(new IdentityServiceException("Failed to validate subject token"))
              case None =>
                logger.error("Unable to validate subject token. Request to OpenStack Identity service timed out.")
                Failure(new IdentityServiceException("OpenStack Identity service could not be reached to validate subject token"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  def getGroups(userId: String, tracingHeader: Option[String] = None, checkCache: Boolean = true): Try[List[Group]] = {
    getFromCache[mutable.ArrayBuffer[Group]](GROUPS_KEY_PREFIX + userId) match {
      case Some(cachedGroups) =>
        Success(cachedGroups.toList)
      case None =>
        getAdminToken(tracingHeader, checkCache) match {
          case Success(adminToken) =>
            val requestTracingHeader = tracingHeader
              .map(headerValue => Map(CommonHttpHeader.TRACE_GUID.toString -> headerValue))
              .getOrElse(Map())
            val headerMap = Map(
              OpenStackIdentityV3Headers.X_AUTH_TOKEN -> adminToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            ) ++ requestTracingHeader
            val groupsResponse = Option(akkaServiceClient.get(
              GROUPS_KEY_PREFIX + userId,
              identityServiceUri + GROUPS_ENDPOINT(userId),
              headerMap.asJava
            ))

            // Since we *might* get a null back from the akka service client, we have to map it, and then match
            // because we care to match on the status code of the response, if anything was set.
            groupsResponse.map(response => response.getStatus) match {
              case Some(statusCode) if statusCode == HttpServletResponse.SC_OK =>
                val json = Json.parse(inputStreamToString(groupsResponse.get.getData))

                val groups = (json \ "groups").as[JsArray].value.map(jsGroup =>
                  Group(
                    (jsGroup \ "id").as[String],
                    (jsGroup \ "name").as[String],
                    (jsGroup \ "description").asOpt[String],
                    (jsGroup \ "domain_id").asOpt[String])).toList

                val offsetConfiguredTtl = offsetTtl(groupsCacheTtl, cacheOffset)
                val ttl = if (offsetConfiguredTtl < 1) {
                  logger.error("Offset group cache ttl was negative, defaulting to 10 minutes. Please check your configuration.")
                  600000
                } else {
                  offsetConfiguredTtl
                }
                logger.debug(s"Caching groups for user '$userId' with TTL set to: ${ttl}ms")
                // TODO: Maybe handle all this conversion jank?
                datastore.put(GROUPS_KEY_PREFIX + userId, groups.toBuffer.asInstanceOf[Serializable], ttl, TimeUnit.MILLISECONDS)

                Success(groups)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_NOT_FOUND =>
                logger.error("Groups for '" + userId + "' not found. Response Code: 404")
                Failure(new InvalidUserForGroupsException("Failed to fetch groups"))
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && checkCache =>
                logger.error("Request made with an expired admin token. Fetching a fresh admin token and retrying groups retrieval. Response Code: 401")
                getGroups(userId, tracingHeader, checkCache = false)
              case Some(statusCode) if statusCode == HttpServletResponse.SC_UNAUTHORIZED && !checkCache =>
                logger.error(s"Retry after fetching a new admin token failed. Aborting groups retrieval for: '$userId'")
                Failure(new IdentityServiceException("Valid admin token could not be fetched"))
              case Some(statusCode) if statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE || statusCode == SC_TOO_MANY_REQUESTS =>
                logger.error(s"OpenStack Identity service returned an Over Limit response status code. Response Code: $statusCode")
                Failure(buildIdentityServiceOverLimitException(groupsResponse.get))
              case Some(statusCode) =>
                logger.error(s"OpenStack Identity service returned an unexpected response status code. Response Code: $statusCode")
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

  private def safeLongToInt(l: Long) =
    math.min(l, Int.MaxValue).toInt

  private def offsetTtl(exactTtl: Int, offset: Int) = {
    if (offset == 0 || exactTtl == 0) exactTtl
    else safeLongToInt(exactTtl.toLong + (Random.nextInt(offset * 2) - offset))
  }

  private def buildIdentityServiceOverLimitException(serviceClientResponse: ServiceClientResponse): IdentityServiceOverLimitException = {
    val statusCode: Int = serviceClientResponse.getStatus
    val retryHeaders = serviceClientResponse.getHeaders.filter { header => header.getName.equals(HttpHeaders.RETRY_AFTER) }
    if (retryHeaders.isEmpty) {
      logger.info(s"Missing ${HttpHeaders.RETRY_AFTER} header on OpenStack Identity Response status code: $statusCode")
      val retryCalendar = new GregorianCalendar()
      retryCalendar.add(Calendar.SECOND, 5)
      val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
      new IdentityServiceOverLimitException("Rate limited by OpenStack Identity service", statusCode, retryString)
    } else {
      new IdentityServiceOverLimitException("Rate limited by OpenStack Identity service", statusCode, retryHeaders.head.getValue)
    }
  }
}
