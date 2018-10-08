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
package org.openrepose.filters.samlpolicy

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletResponse.{SC_OK, SC_REQUEST_ENTITY_TOO_LARGE, SC_UNAUTHORIZED}
import javax.ws.rs.core.MediaType
import org.apache.http.HttpHeaders
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{CloseableHttpResponse, RequestBuilder}
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.springframework.web.util.UriUtils
import play.api.libs.json.{JsArray, Json}

import scala.Function.tupled
import scala.util.{Failure, Success, Try}

/**
  * Handles all of the API interactions necessary for the [[SamlPolicyTranslationFilter]].
  *
  * The default singleton scope is used, and works, due entirely to the separation of
  * Spring contexts between filters.
  */
@Named
class SamlIdentityClient @Inject()(httpClientService: HttpClientService) {

  import SamlIdentityClient._

  private var tokenUri: String = _
  private var policyUri: String = _
  private var tokenServiceClient: ServiceClient = _
  private var policyServiceClient: ServiceClient = _

  /**
    * Obtains service clients for the provided connection pool IDs.
    * These service clients will be used when making calls to external APIs.
    */
  def using(tokenUri: String, policyUri: String, tokenPoolId: Option[String], policyPoolId: Option[String]): Unit = {
    this.tokenUri = tokenUri
    this.policyUri = policyUri

    val optTokenServiceClient = Option(tokenServiceClient)
    if (optTokenServiceClient.isEmpty || tokenServiceClient.poolId != tokenPoolId) {
      tokenServiceClient = ServiceClient(
        tokenPoolId,
        tokenPoolId.map(httpClientService.getClient)
          .getOrElse(httpClientService.getDefaultClient)
      )
    }

    val optPolicyServiceClient = Option(policyServiceClient)
    if (optPolicyServiceClient.isEmpty || policyServiceClient.poolId != policyPoolId) {
      policyServiceClient = ServiceClient(
        policyPoolId,
        policyPoolId.map(httpClientService.getClient)
          .getOrElse(httpClientService.getDefaultClient)
      )
    }
  }

  /**
    * Retrieves a token from Identity that will be used for authorization on all other calls to Identity.
    *
    * @param username the username on the account
    * @param password the password on the account
    * @param traceId  an optional identifier to be sent with the request
    * @return the token if successful, or a failure if unsuccessful
    */
  def getToken(username: String, password: String, traceId: Option[String]): Try[String] = {
    val authenticationPayload = Json.obj(
      "auth" -> Json.obj(
        "passwordCredentials" -> Json.obj(
          "username" -> username,
          "password" -> password
        )
      )
    )

    val requestHeaders = Map(HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON) ++
      traceId.map(CommonHttpHeader.TRACE_GUID.->)
    val requestEntity = EntityBuilder.create()
      .setText(Json.stringify(authenticationPayload))
      .setContentType(ContentType.APPLICATION_JSON)
      .build()
    val requestBuilder = RequestBuilder.post(s"$tokenUri$TokenPath")
      .setEntity(requestEntity)
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(TokenRequestKey)

    val tryResponse = Try(tokenServiceClient.httpClient.execute(request, cacheContext))

    tryResponse match {
      case Success(httpResponse) =>
        httpResponse.getStatusLine.getStatusCode match {
          case SC_OK =>
            Try {
              val json = Json.parse(EntityUtils.toString(httpResponse.getEntity))
              (json \ "access" \ "token" \ "id").as[String]
            } recover {
              case f: Exception =>
                throw GenericIdentityException("Token could not be parsed from response from Identity", f)
            }
          case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS =>
            Failure(OverLimitException(buildRetryValue(httpResponse), "Rate limited when getting token"))
          case statusCode =>
            Failure(UnexpectedStatusCodeException(statusCode, "Unexpected response from Identity"))
        }
      case Failure(f) =>
        Failure(GenericIdentityException("Failure communicating with Identity when getting token", f))
    }
  }

  /**
    * Retrieves a IDP-ID from Identity that will be used to fetch the policy.
    *
    * @param issuer     the issuer associated with the Identity provider
    * @param traceId    an optional identifier to be sent with the request
    * @param checkCache whether or not to use the HTTP request cache
    * @return the IDP ID if successful, or a failure if unsuccessful
    */
  def getIdpId(issuer: String, token: String, traceId: Option[String], checkCache: Boolean): Try[ProviderInfo] = {
    val requestHeaders = Map(
      HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON,
      CommonHttpHeader.AUTH_TOKEN -> token) ++
      traceId.map(CommonHttpHeader.TRACE_GUID.->)
    val requestBuilder = RequestBuilder.get(s"$policyUri${IdpPath(issuer)}")
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(IdpRequestKey(issuer))
      .setUseCache(checkCache)

    val tryResponse = Try(policyServiceClient.httpClient.execute(request, cacheContext))

    tryResponse match {
      case Success(httpResponse) =>
        httpResponse.getStatusLine.getStatusCode match {
          case SC_OK =>
            Try {
              val json = Json.parse(EntityUtils.toString(httpResponse.getEntity))
              val idp = (json \ "RAX-AUTH:identityProviders").as[JsArray].value
                .headOption
                .getOrElse(throw SamlPolicyException(SC_UNAUTHORIZED, "Unknown issuer"))
              ProviderInfo((idp \ "id").as[String], (idp \ "approvedDomainIds").as[Array[String]])
            } recover {
              case s: SamlPolicyException =>
                throw s
              case f: Exception =>
                throw GenericIdentityException("IDP ID could not be parsed from response from Identity", f)
            }
          case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS =>
            Failure(OverLimitException(buildRetryValue(httpResponse), "Rate limited when getting IDP ID"))
          case statusCode =>
            Failure(UnexpectedStatusCodeException(statusCode, "Unexpected response from Identity"))
        }
      case Failure(f) =>
        Failure(GenericIdentityException("Failure communicating with Identity when getting IDP ID", f))
    }
  }

  /**
    * Retrieves a policy from Identity that will be used during SAMLResponse translation.
    *
    * @param token      the token to be sent with the request, used in authorization
    * @param traceId    an optional identifier to be sent with the request
    * @param checkCache whether or not to use the HTTP request cache
    * @return the policy if successful, or a failure if unsuccessful
    */
  def getPolicy(provider: ProviderInfo, token: String, traceId: Option[String], checkCache: Boolean): Try[Policy] = {
    val requestHeaders = Map(
      HttpHeaders.ACCEPT -> String.join(",", TextYaml, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML),
      CommonHttpHeader.AUTH_TOKEN -> token) ++
      traceId.map(CommonHttpHeader.TRACE_GUID.->)
    val requestBuilder = RequestBuilder.get(s"$policyUri${PolicyPath(provider.idpId)}")
    requestHeaders.foreach(tupled(requestBuilder.addHeader))
    val request = requestBuilder.build()

    val cacheContext = CachingHttpClientContext.create()
      .setCacheKey(PolicyRequestKey(provider.idpId))
      .setUseCache(checkCache)

    val tryResponse = Try(policyServiceClient.httpClient.execute(request, cacheContext))

    tryResponse match {
      case Success(httpResponse) =>
        httpResponse.getStatusLine.getStatusCode match {
          case SC_OK =>
            Try {
              //Option(httpResponse.getEntity.getContentType).map(_.getValue)
              val contentTypeHeader = Try(ContentType.get(httpResponse.getEntity)).map(_.getMimeType)
              val content = EntityUtils.toString(httpResponse.getEntity)
              Policy(
                content,
                contentTypeHeader.getOrElse(TextYaml),
                provider.domains)
            } recover {
              case f: Exception =>
                throw GenericIdentityException("Policy in response from Identity could not be read", f)
            }
          case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS =>
            Failure(OverLimitException(buildRetryValue(httpResponse), "Rate limited when getting policy"))
          case statusCode =>
            Failure(UnexpectedStatusCodeException(statusCode, "Unexpected response from Identity"))
        }
      case Failure(f) =>
        Failure(GenericIdentityException("Failure communicating with Identity when getting policy", f))
    }
  }

  private case class ServiceClient(poolId: Option[String], httpClient: HttpClientServiceClient)

}

object SamlIdentityClient {
  final val SC_TOO_MANY_REQUESTS: Int = 429
  final val TextYaml: String = "text/yaml"
  final val TokenRequestKey: String = "SAML:TOKEN"
  final val IdpRequestKey: (String) => String = (issuer: String) => s"SAML:IDP:$issuer"
  final val PolicyRequestKey: (String) => String = (idpId: String) => s"SAML:POLICY:$idpId"
  final val TokenPath: String = "/v2.0/tokens"
  final val IdpPath: (String) => String =
    (issuer: String) => s"/v2.0/RAX-AUTH/federation/identity-providers?issuer=${UriUtils.encodeQueryParam(issuer, StandardCharsets.UTF_8.name())}"
  final val PolicyPath: (String) => String =
    (idpId: String) => s"/v2.0/RAX-AUTH/federation/identity-providers/$idpId/mapping"

  def buildRetryValue(response: CloseableHttpResponse): String = {
    response.getHeaders(HttpHeaders.RETRY_AFTER).headOption.map(_.getValue)
      .getOrElse(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")).plusSeconds(5)))
  }

  trait IdentityException extends Exception

  case class GenericIdentityException(message: String, cause: Throwable)
    extends Exception(message, cause) with IdentityException

  case class UnexpectedStatusCodeException(statusCode: Int, message: String)
    extends Exception(message)

  case class UnsupportedPolicyFormatException(message: String)
    extends Exception(message) with IdentityException

  case class OverLimitException(retryAfter: String, message: String)
    extends Exception(message) with IdentityException

  case class Policy(content: String, contentType: String, domains: Array[String])

  case class ProviderInfo(idpId: String, domains: Array[String])

}
