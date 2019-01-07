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

import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.servlet.http.HttpServletResponse._
import javax.ws.rs.core.MediaType
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{CloseableHttpResponse, RequestBuilder}
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import org.openrepose.commons.utils.http.{CommonHttpHeader, HttpDate}
import org.openrepose.core.services.datastore.Datastore
import org.openrepose.core.services.datastore.types.SetPatch
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientServiceClient}
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config
import org.openrepose.filters.openstackidentityv3.objects._
import org.openrepose.filters.openstackidentityv3.utilities.Cache._
import org.springframework.http.HttpHeaders
import play.api.libs.json._

import scala.Function.tupled
import scala.io.Source
import scala.util.{Failure, Success, Try}

class OpenStackIdentityV3API(config: OpenstackIdentityV3Config, datastore: Datastore, httpClient: HttpClientServiceClient)
  extends StrictLogging {

  private final val SC_TOO_MANY_REQUESTS = 429
  private final val TOKEN_ENDPOINT = "/v3/auth/tokens"
  private final val GROUPS_ENDPOINT = (userId: String) => s"/v3/users/$userId/groups"

  private val identityServiceUri = config.getOpenstackIdentityService.getUri
  private val timeouts = Option(config.getCache).flatMap(cache => Option(cache.getTimeouts))
  private val cacheOffset =
    timeouts.map(_.getVariance).flatMap(Option.apply).getOrElse(0)
  // TODO: The Token and Group Cache TTL defaults (i.e. 600) will need moved to the XSD when the deprecated elements
  // TODO: are removed and the attributes can directly have defaults.
  private val tokenCacheTtl =
  timeouts.flatMap(timeout => Option(timeout.getToken).orElse(Option(timeout.getTokenElement)))
    .map(_.intValue)
    .getOrElse(600)

  private val groupsCacheTtl =
    timeouts.flatMap(timeout => Option(timeout.getGroup).orElse(Option(timeout.getGroupElement)))
      .map(_.intValue)
      .getOrElse(600)

  def getAdminToken(tracingHeader: Option[String] = None, checkCache: Boolean = true): Try[String] = {
    def createAdminAuthRequest() = {
      val username = config.getOpenstackIdentityService.getUsername
      val password = config.getOpenstackIdentityService.getPassword

      // don't include these fields at all if they're going to be null
      val domain = Option(config.getOpenstackIdentityService.getDomainId).map(id => Seq(
        "domain" -> Json.obj(
          "id" -> id
        )
      )) getOrElse Seq.empty

      val scope = Option(config.getOpenstackIdentityService.getProjectId).map(id => Seq(
        "scope" -> Json.obj(
          "project" -> Json.obj(
            "id" -> id
          )
        )
      )) getOrElse Seq.empty

      Json.stringify(JsObject(Seq(
        "auth" -> JsObject(Seq(
          "identity" -> JsObject(Seq(
            "methods" -> JsArray(Seq(JsString("password"))),
            "password" -> JsObject(Seq(
              "user" -> JsObject(
                domain ++ Seq(
                  "name" -> JsString(username),
                  "password" -> JsString(password)
                ))))
          ))) ++
          scope
        )
      )))
    }

    Option(datastore.get(AdminTokenKey).asInstanceOf[String]) match {
      case Some(adminToken) if checkCache =>
        Success(adminToken)
      case _ =>
        val requestTracingHeader = tracingHeader
          .map(headerValue => Map(CommonHttpHeader.TRACE_GUID -> headerValue))
          .getOrElse(Map())
        val headerMap = Map(HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON) ++ requestTracingHeader

        val requestBody = EntityBuilder.create()
          .setText(createAdminAuthRequest())
          .setContentType(ContentType.APPLICATION_JSON)
          .build()
        val requestBuilder = RequestBuilder.post(identityServiceUri + TOKEN_ENDPOINT)
          .setEntity(requestBody)
        headerMap.foreach(tupled(requestBuilder.addHeader))
        val request = requestBuilder.build()

        val cachingContext = CachingHttpClientContext.create()
          .setCacheKey(AdminTokenKey)

        val authTokenResponse = Try(httpClient.execute(request, cachingContext))

        // Since the HTTP client *might* throw an exception, we have to map it, and then match
        // because we care to match on the status code of the response.
        authTokenResponse.map(_.getStatusLine.getStatusCode) match {
          case Success(statusCode) if statusCode == SC_CREATED =>
            authTokenResponse.get.getHeaders(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN).headOption.map(_.getValue) match {
              case Some(token) =>
                logger.debug("Caching admin token")

                val json = Json.parse(EntityUtils.toString(authTokenResponse.get.getEntity))
                val tokenExpiration = (json \ "token" \ "expires_at").as[String]
                val adminTokenTtl = safeLongToInt(new DateTime(tokenExpiration).getMillis - DateTime.now.getMillis)
                logger.debug(s"Caching admin token with TTL set to: ${adminTokenTtl}ms")

                datastore.put(AdminTokenKey, token, adminTokenTtl, TimeUnit.MILLISECONDS)
                Success(token)
              case None =>
                logger.error("Headers not found in a successful response to an admin token request. The OpenStack Identity service is not adhering to the v3 contract.")
                Failure(new IdentityServiceException("OpenStack Identity service did not return headers with a successful response"))
            }
          case Success(statusCode) if statusCode == SC_REQUEST_ENTITY_TOO_LARGE || statusCode == SC_TOO_MANY_REQUESTS =>
            logger.error(s"Unable to get admin token. OpenStack Identity service returned an Over Limit response status code. Response Code: $statusCode")
            Failure(buildIdentityServiceOverLimitException(authTokenResponse.get))
          case Success(statusCode) =>
            logger.error(s"Unable to get admin token. Please verify your admin credentials. Response Code: $statusCode")
            Failure(new InvalidAdminCredentialsException("Failed to fetch admin token"))
          case Failure(_) =>
            logger.error("Unable to get admin token from the OpenStack Identity service.")
            Failure(new IdentityServiceException("OpenStack Identity service could not be reached to obtain admin token"))
        }
    }
  }

  def validateToken(subjectToken: String, tracingHeader: Option[String] = None, checkCache: Boolean = true): Try[ValidToken] = {
    Option(datastore.get(getTokenKey(subjectToken)).asInstanceOf[ValidToken]) match {
      case Some(cachedSubjectTokenObject) =>
        Success(cachedSubjectTokenObject)
      case None =>
        getAdminToken(tracingHeader, checkCache) match {
          case Success(adminToken) =>
            val requestTracingHeader = tracingHeader
              .map(headerValue => Map(CommonHttpHeader.TRACE_GUID -> headerValue))
              .getOrElse(Map())
            val headerMap = Map(
              OpenStackIdentityV3Headers.X_AUTH_TOKEN -> adminToken,
              OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> subjectToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            ) ++ requestTracingHeader

            val requestBuilder = RequestBuilder.get(identityServiceUri + TOKEN_ENDPOINT)
            headerMap.foreach(tupled(requestBuilder.addHeader))
            val request = requestBuilder.build()

            val cachingContext = CachingHttpClientContext.create()
              .setCacheKey(getTokenKey(subjectToken))

            val validateTokenResponse = Try(httpClient.execute(request, cachingContext))

            // Since the HTTP client *might* throw an exception, we have to map it, and then match
            // because we care to match on the status code of the response.
            validateTokenResponse.map(_.getStatusLine.getStatusCode) match {
              case Success(statusCode) if statusCode == SC_OK =>
                val json = Json.parse(EntityUtils.toString(validateTokenResponse.get.getEntity))

                val subjectTokenObject = ValidToken(
                  userId = (json \ "token" \ "user" \ "id").asOpt[String],
                  userName = (json \ "token" \ "user" \ "name").asOpt[String],
                  defaultRegion = (json \ "token" \ "user" \ "RAX-AUTH:defaultRegion").asOpt[String],
                  expiresAt = (json \ "token" \ "expires_at").as[String],
                  projectId = (json \ "token" \ "project" \ "id").asOpt[String],
                  projectName = (json \ "token" \ "project" \ "name").asOpt[String],
                  catalogJson = (json \ "token" \ "catalog").toOption.map(_.toString),
                  catalogEndpoints = (json \ "token" \ "catalog" \\ "endpoints")
                    .flatMap(_.as[JsArray].value.map(jsEndpoint =>
                      Endpoint(
                        (jsEndpoint \ "name").asOpt[String],
                        (jsEndpoint \ "interface").asOpt[String],
                        (jsEndpoint \ "region").asOpt[String],
                        (jsEndpoint \ "url").as[String])
                    )).toList,
                  roles = (json \ "token" \ "roles").asOpt[JsArray].map(_.value.map(jsRole =>
                    Role(
                      (jsRole \ "name").as[String],
                      (jsRole \ "project_id").asOpt[String],
                      (jsRole \ "RAX-AUTH:project_id").asOpt[String])).toList) getOrElse List.empty,
                  impersonatorId = (json \ "token" \ "RAX-AUTH:impersonator" \ "id").asOpt[String],
                  impersonatorName = (json \ "token" \ "RAX-AUTH:impersonator" \ "name").asOpt[String])

                val identityTtl = safeLongToInt(new DateTime(subjectTokenObject.expiresAt).getMillis - DateTime.now.getMillis)
                val offsetConfiguredTtl = offsetTtl(tokenCacheTtl, cacheOffset)
                // TODO: Come up with a better algorithm to decide the cache TTL and handle negative/0 TTLs
                val ttl = if (offsetConfiguredTtl < 1) identityTtl else math.max(math.min(offsetConfiguredTtl, identityTtl), 1)
                logger.debug(s"Caching token '$subjectToken' with TTL set to: ${ttl} seconds")
                subjectTokenObject.userId foreach { userId =>
                  datastore.patch(getUserIdKey(userId), new SetPatch(subjectToken), ttl, TimeUnit.SECONDS)
                }
                datastore.put(getTokenKey(subjectToken), subjectTokenObject, ttl, TimeUnit.SECONDS)

                Success(subjectTokenObject)
              case Success(statusCode) if statusCode == SC_NOT_FOUND =>
                logger.error("Subject token validation failed. Response Code: 404")
                Failure(new InvalidSubjectTokenException("Failed to validate subject token"))
              case Success(statusCode) if statusCode == SC_UNAUTHORIZED && checkCache =>
                logger.error("Request made with an expired admin token. Fetching a fresh admin token and retrying token validation. Response Code: 401")
                validateToken(subjectToken, tracingHeader, checkCache = false)
              case Success(statusCode) if statusCode == SC_UNAUTHORIZED && !checkCache =>
                logger.error(s"Retry after fetching a new admin token failed. Aborting subject token validation for: '$subjectToken'")
                Failure(new IdentityServiceException("Valid admin token could not be fetched"))
              case Success(statusCode) if statusCode == SC_REQUEST_ENTITY_TOO_LARGE || statusCode == SC_TOO_MANY_REQUESTS =>
                logger.error(s"OpenStack Identity service returned an Over Limit response status code. Response Code: $statusCode")
                Failure(buildIdentityServiceOverLimitException(validateTokenResponse.get))
              case Success(statusCode) =>
                logger.error(s"OpenStack Identity service returned an unexpected response status code. Response Code: $statusCode")
                Failure(new IdentityServiceException("Failed to validate subject token"))
              case Failure(_) =>
                logger.error("Unable to validate subject token with the OpenStack Identity service.")
                Failure(new IdentityServiceException("OpenStack Identity service could not be reached to validate subject token"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  def getGroups(userId: String, subjectToken: String, tracingHeader: Option[String] = None, checkCache: Boolean = true): Try[List[String]] = {
    Option(datastore.get(getGroupsKey(subjectToken)).asInstanceOf[List[String]]) match {
      case Some(cachedGroups) =>
        Success(cachedGroups)
      case None =>
        getAdminToken(tracingHeader, checkCache) match {
          case Success(adminToken) =>
            val requestTracingHeader = tracingHeader
              .map(headerValue => Map(CommonHttpHeader.TRACE_GUID -> headerValue))
              .getOrElse(Map())
            val headerMap = Map(
              OpenStackIdentityV3Headers.X_AUTH_TOKEN -> adminToken,
              HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON
            ) ++ requestTracingHeader

            val requestBuilder = RequestBuilder.get(identityServiceUri + GROUPS_ENDPOINT(userId))
            headerMap.foreach(tupled(requestBuilder.addHeader))
            val request = requestBuilder.build()

            val cachingContext = CachingHttpClientContext.create()
              .setCacheKey(getGroupsKey(subjectToken))

            val groupsResponse = Try(httpClient.execute(request, cachingContext))

            // Since the HTTP client *might* throw an exception, we have to map it, and then match
            // because we care to match on the status code of the response.
            groupsResponse.map(_.getStatusLine.getStatusCode) match {
              case Success(statusCode) if statusCode == SC_OK =>
                val json = Json.parse(EntityUtils.toString(groupsResponse.get.getEntity))
                val groups = (json \ "groups" \\ "name").map(_.as[String]).toList
                val offsetConfiguredTtl = offsetTtl(groupsCacheTtl, cacheOffset)
                val ttl = if (offsetConfiguredTtl < 1) {
                  logger.error("Offset group cache ttl was negative, defaulting to 10 minutes. Please check your configuration.")
                  600000
                } else {
                  offsetConfiguredTtl
                }
                logger.debug(s"Caching groups for user '$userId' with TTL set to: ${ttl} seconds")
                // TODO: Maybe handle all this conversion jank?
                datastore.put(getGroupsKey(subjectToken), groups.asInstanceOf[Serializable], ttl, TimeUnit.SECONDS)

                Success(groups)
              case Success(statusCode) if statusCode == SC_NOT_FOUND =>
                logger.error("Groups for '" + userId + "' not found. Response Code: 404")
                Failure(new InvalidUserForGroupsException("Failed to fetch groups"))
              case Success(statusCode) if statusCode == SC_UNAUTHORIZED && checkCache =>
                logger.error("Request made with an expired admin token. Fetching a fresh admin token and retrying groups retrieval. Response Code: 401")
                getGroups(userId, subjectToken, tracingHeader, checkCache = false)
              case Success(statusCode) if statusCode == SC_UNAUTHORIZED && !checkCache =>
                logger.error(s"Retry after fetching a new admin token failed. Aborting groups retrieval for: '$userId'")
                Failure(new IdentityServiceException("Valid admin token could not be fetched"))
              case Success(statusCode) if statusCode == SC_REQUEST_ENTITY_TOO_LARGE || statusCode == SC_TOO_MANY_REQUESTS =>
                logger.error(s"OpenStack Identity service returned an Over Limit response status code. Response Code: $statusCode")
                Failure(buildIdentityServiceOverLimitException(groupsResponse.get))
              case Success(statusCode) =>
                logger.error(s"OpenStack Identity service returned an unexpected response status code. Response Code: $statusCode")
                Failure(new IdentityServiceException("Failed to fetch groups"))
              case Failure(_) =>
                logger.error("Unable to get groups from the OpenStack Identity service.")
                Failure(new IdentityServiceException("OpenStack Identity service could not be reached to obtain groups"))
            }
          case Failure(e) => Failure(e)
        }
    }
  }

  private def inputStreamToString(inputStream: InputStream) = {
    inputStream.reset() // TODO: Remove this when we can. It relies on our implementation returning an InputStream that supports reset.
    Source.fromInputStream(inputStream).mkString
  }

  private def buildIdentityServiceOverLimitException(closeableHttpResponse: CloseableHttpResponse): IdentityServiceOverLimitException = {
    val statusCode: Int = closeableHttpResponse.getStatusLine.getStatusCode
    closeableHttpResponse.getHeaders(HttpHeaders.RETRY_AFTER).headOption.map(_.getValue) match {
      case Some(retryValue) => new IdentityServiceOverLimitException("Rate limited by OpenStack Identity service", statusCode, retryValue)
      case _ =>
        logger.info(s"Missing ${HttpHeaders.RETRY_AFTER} header on OpenStack Identity Response status code: $statusCode")
        val retryCalendar = new GregorianCalendar()
        retryCalendar.add(Calendar.SECOND, 5)
        val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
        new IdentityServiceOverLimitException("Rate limited by OpenStack Identity service", statusCode, retryString)
    }
  }
}
