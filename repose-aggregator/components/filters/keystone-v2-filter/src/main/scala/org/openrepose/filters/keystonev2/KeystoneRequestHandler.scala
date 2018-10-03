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
package org.openrepose.filters.keystonev2

import java.io.InputStream
import java.util.{Calendar, GregorianCalendar}

import com.fasterxml.jackson.core.JsonProcessingException
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.servlet.http.HttpServletResponse._
import javax.ws.rs.core.{HttpHeaders, MediaType}
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.DateUtils
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.joda.time.format.ISODateTimeFormat
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.core.services.httpclient.CachingHttpClientContext
import org.openrepose.filters.keystonev2.KeystoneV2Common.{Endpoint, EndpointsData, Role, ValidToken}
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.Function.tupled
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Contains the functions which interact with the Keystone API.
  */
class KeystoneRequestHandler(identityServiceUri: String, httpClient: HttpClient, traceId: Option[String])
  extends StrictLogging {

  import KeystoneRequestHandler._

  /**
    * Call to the Keystone service to get the admin token
    *
    * Note that the checkCache parameter was provided for the sake of consistency across the API of this class.
    * Generally speaking, checkCache should have a value of true. The reason is that the admin token is shared by
    * all users, whereas a token, groups, or endpoints are user specific. If checkCache were to be false on retry,
    * then every time the admin token expires, every user request currently in flight is likely to force a unique
    * connection to be made to the Identity service. That is exactly the problem that the Akka service client was
    * made to solve.
    *
    * @return a Successful token, or a Failure
    */
  final def getAdminToken(adminUsername: String, adminPassword: String, checkCache: Boolean = true): Try[String] = {
    //authenticate, or get the admin token
    val authenticationPayload = Json.obj(
      "auth" -> Json.obj(
        "passwordCredentials" -> Json.obj(
          "username" -> adminUsername,
          "password" -> adminPassword
        )
      )
    )

    val requestHeaders = Map(HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON)
      .++(traceId.map(CommonHttpHeader.TRACE_GUID -> _))
    val requestEntity = EntityBuilder.create()
      .setText(Json.stringify(authenticationPayload))
      .setContentType(ContentType.APPLICATION_JSON)
      .build()
    val requestBuilder = RequestBuilder.post(s"$identityServiceUri$TOKEN_ENDPOINT")
      .setEntity(requestEntity)
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(ADMIN_TOKEN_KEY)
      .setUseCache(checkCache)

    val tryResponse = Try(httpClient.execute(request, cacheContext))

    tryResponse match {
      case Success(response) =>
        response.getStatusLine.getStatusCode match {
          case statusCode if statusCode >= 200 && statusCode < 300 =>
            val responseEntity = response.getEntity
            try {
              // todo: test character encoding resolution
              val json = Json.parse(responseEntity.getContent)
              Try(Success((json \ "access" \ "token" \ "id").as[String])) match {
                case Success(s) => s
                case Failure(f) =>
                  Failure(IdentityCommunicationException("Token not found in identity response during admin authentication", f))
              }
            } finally {
              EntityUtils.consumeQuietly(responseEntity)
            }
          case statusCode@(SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS) =>
            Failure(OverLimitException(statusCode, buildRetryValue(response), "Rate limited when getting admin token"))
          case statusCode if statusCode >= 500 =>
            Failure(IdentityCommunicationException("Identity Service not available to get admin token"))
          case _ => Failure(IdentityResponseProcessingException("Unable to successfully get admin token from Identity"))
        }
      case Failure(x) => Failure(IdentityResponseProcessingException("Failure communicating with identity during admin authentication", x))
    }
  }

  final def validateToken(validatingToken: String, validatableToken: String, applyRcnRoles: Boolean, ignoredRoles: Set[String], checkCache: Boolean = true): Try[ValidToken] = {
    def extractUserInformation(keystoneResponse: InputStream): Try[ValidToken] = {
      // TODO: Handle character encoding set in the content-type header rather than relying on the default system encoding
      val input: String = Source.fromInputStream(keystoneResponse).getLines mkString ""
      try {
        val json = Json.parse(input)
        //Have to convert it to a vector, because List isn't serializeable in 2.10
        val userId = (json \ "access" \ "user" \ "id").as[String]
        val roles = (json \ "access" \ "user" \ "roles").as[JsArray].value
          .map(jsRole => Role((jsRole \ "name").as[String], (jsRole \ "tenantId").asOpt[String]))
          .filter(role => !ignoredRoles.contains(role.name))
          .toVector
        val authenticatedBy = (json \ "access" \ "token" \ "RAX-AUTH:authenticatedBy").asOpt[JsArray]
          .map(_.value)
          .map(_.map(_.as[String]))
          .map(_.toVector)
        val expirationDate = iso8601ToRfc1123((json \ "access" \ "token" \ "expires").as[String])
        val username = (json \ "access" \ "user" \ "name").asOpt[String]
        val defaultTenantId = (json \ "access" \ "token" \ "tenant" \ "id").asOpt[String]
        val tenantIds = roles.flatMap(_.tenantId)
        val tenantName = (json \ "access" \ "token" \ "tenant" \ "name").asOpt[String]
        val domainId = (json \ "access" \ "user" \ "RAX-AUTH:domainId").asOpt[String]
        val defaultRegion = (json \ "access" \ "user" \ "RAX-AUTH:defaultRegion").asOpt[String]
        val contactId = (json \ "access" \ "user" \ "RAX-AUTH:contactId").asOpt[String]
        val impersonatorId = (json \ "access" \ "RAX-AUTH:impersonator" \ "id").asOpt[String]
        val impersonatorName = (json \ "access" \ "RAX-AUTH:impersonator" \ "name").asOpt[String]
        val impersonatorRoles = (json \ "access" \ "RAX-AUTH:impersonator" \ "roles" \\ "name").map(_.as[String]).toVector
        val validToken = ValidToken(expirationDate,
          userId,
          roles,
          username,
          tenantName,
          defaultTenantId,
          tenantIds,
          impersonatorId,
          impersonatorName,
          impersonatorRoles,
          domainId,
          defaultRegion,
          contactId,
          authenticatedBy)

        Success(validToken)
      } catch {
        case oops@(_: JsResultException | _: JsonProcessingException) =>
          Failure(IdentityCommunicationException("Unable to parse JSON from identity validate token response", oops))
      }
    }

    val requestHeaders = Map(
      CommonHttpHeader.AUTH_TOKEN -> validatingToken,
      HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON)
      .++(traceId.map(CommonHttpHeader.TRACE_GUID -> _))
    val requestBuilder = RequestBuilder.get(s"$identityServiceUri$TOKEN_ENDPOINT/$validatableToken${getApplyRcnRoles(applyRcnRoles)}")
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(s"$TOKEN_KEY_PREFIX$validatableToken")
      .setUseCache(checkCache)

    val tryResponse = Try(httpClient.execute(request, cacheContext))

    handleResponse("validate token", tryResponse, extractUserInformation)
  }

  final def getEndpointsForToken(authenticatingToken: String, forToken: String, applyRcnRoles: Boolean, checkCache: Boolean = true): Try[EndpointsData] = {
    def extractEndpointInfo(inputStream: InputStream): Try[EndpointsData] = {
      // TODO: Handle character encoding set in the content-type header rather than relying on the default system encoding
      val jsonString = Source.fromInputStream(inputStream).getLines mkString ""
      val json = Json.parse(jsonString)

      //Have to convert it to a vector, because List isn't serializeable in 2.10
      (json \ "endpoints").validate[Vector[Endpoint]] match {
        case s: JsSuccess[Vector[Endpoint]] =>
          val endpoints = s.get
          Success(EndpointsData(jsonString, endpoints))
        case _: JsError =>
          Failure(IdentityCommunicationException("Identity didn't respond with proper Endpoints JSON"))
      }
    }

    val requestHeaders = Map(
      CommonHttpHeader.AUTH_TOKEN -> authenticatingToken,
      HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON)
      .++(traceId.map(CommonHttpHeader.TRACE_GUID -> _))
    val requestBuilder = RequestBuilder.get(s"$identityServiceUri${ENDPOINTS_ENDPOINT(forToken)}${getApplyRcnRoles(applyRcnRoles)}")
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(s"$ENDPOINTS_KEY_PREFIX$forToken")
      .setUseCache(checkCache)

    val tryResponse = Try(httpClient.execute(request, cacheContext))

    handleResponse("endpoints", tryResponse, extractEndpointInfo)
  }

  final def getGroups(authenticatingToken: String, forToken: String, checkCache: Boolean = true): Try[Vector[String]] = {
    def extractGroupInfo(inputStream: InputStream): Try[Vector[String]] = {
      Try {
        // TODO: Handle character encoding set in the content-type header rather than relying on the default system encoding
        val input: String = Source.fromInputStream(inputStream).getLines mkString ""
        val json = Json.parse(input)

        (json \ "RAX-KSGRP:groups" \\ "id").map(_.as[String]).toVector
      }
    }

    val requestHeaders = Map(
      CommonHttpHeader.AUTH_TOKEN -> authenticatingToken,
      HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON)
      .++(traceId.map(CommonHttpHeader.TRACE_GUID -> _))
    val requestBuilder = RequestBuilder.get(s"$identityServiceUri${GROUPS_ENDPOINT(forToken)}")
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(s"$GROUPS_KEY_PREFIX$forToken")
      .setUseCache(checkCache)

    val tryResponse = Try(httpClient.execute(request, cacheContext))

    handleResponse("groups", tryResponse, extractGroupInfo)
  }

  private def getApplyRcnRoles(applyRcnRoles: Boolean): String = {
    if (applyRcnRoles) "?apply_rcn_roles=true" else ""
  }
}

object KeystoneRequestHandler {
  final val SC_TOO_MANY_REQUESTS = 429
  final val TOKEN_ENDPOINT = "/v2.0/tokens"
  final val GROUPS_ENDPOINT: (String) => String = (userId: String) => s"/v2.0/users/$userId/RAX-KSGRP"
  final val ENDPOINTS_ENDPOINT: (String) => String = (token: String) => s"/v2.0/tokens/$token/endpoints"
  final val ADMIN_TOKEN_KEY = "IDENTITY:V2:ADMIN_TOKEN"
  final val TOKEN_KEY_PREFIX = "IDENTITY:V2:TOKEN:"
  final val USER_ID_KEY_PREFIX = "IDENTITY:V2:USER_ID:"
  final val GROUPS_KEY_PREFIX = "IDENTITY:V2:GROUPS:"
  final val ENDPOINTS_KEY_PREFIX = "IDENTITY:V2:ENDPOINTS:"

  def iso8601ToRfc1123(iso: String): String = {
    val dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(iso)
    DateUtils.formatDate(dateTime.toDate)
  }

  def buildRetryValue(response: HttpResponse): String = {
    response.getHeaders(HttpHeaders.RETRY_AFTER).headOption.map(_.getValue).getOrElse {
      val retryCalendar: Calendar = new GregorianCalendar
      retryCalendar.add(Calendar.SECOND, 5)
      DateUtils.formatDate(retryCalendar.getTime)
    }
  }

  def handleResponse[T](call: String, tryResponse: Try[HttpResponse], onSuccess: InputStream => Try[T]): Try[T] = {
    tryResponse match {
      case Success(response) =>
        response.getStatusLine.getStatusCode match {
          case statusCode if statusCode >= 200 && statusCode < 300 =>
            val responseEntity = response.getEntity
            try {
              onSuccess(responseEntity.getContent)
            } finally {
              EntityUtils.consume(responseEntity)
            }
          case SC_BAD_REQUEST => Failure(BadRequestException(s"Bad $call request to identity"))
          case SC_UNAUTHORIZED =>
            Failure(AdminTokenUnauthorizedException(s"Admin token unauthorized to make $call request"))
          case SC_FORBIDDEN => Failure(IdentityAdminTokenException(s"Admin token forbidden from making $call request"))
          case SC_NOT_FOUND => Failure(NotFoundException(s"Resource not found for $call request"))
          case statusCode@(SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS) =>
            Failure(OverLimitException(statusCode, buildRetryValue(response), s"Rate limited when making $call request"))
          case statusCode if statusCode >= 500 =>
            Failure(IdentityCommunicationException(s"Identity Service not available for $call request"))
          case _ => Failure(IdentityResponseProcessingException(s"Unhandled response from Identity for $call request"))
        }
      case Failure(x) => Failure(IdentityResponseProcessingException(s"Failure communicating with Identity during $call request", x))
    }
  }

  abstract class IdentityException(message: String, cause: Throwable) extends Exception(message, cause)

  case class AdminTokenUnauthorizedException(message: String, cause: Throwable = null) extends IdentityException(message, cause)

  case class IdentityAdminTokenException(message: String, cause: Throwable = null) extends IdentityException(message, cause)

  case class IdentityResponseProcessingException(message: String, cause: Throwable = null) extends IdentityException(message, cause)

  case class NotFoundException(message: String, cause: Throwable = null) extends IdentityException(message, cause)

  case class BadRequestException(message: String, cause: Throwable = null) extends IdentityException(message, cause)

  case class IdentityCommunicationException(message: String, cause: Throwable = null) extends IdentityException(message, cause)

  case class OverLimitException(statusCode: Int, retryAfter: String, message: String, cause: Throwable = null) extends IdentityException(message, cause)

}
