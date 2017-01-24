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
import javax.servlet.http.HttpServletResponse.{SC_OK, SC_REQUEST_ENTITY_TOO_LARGE}
import javax.ws.rs.core.MediaType

import org.openrepose.commons.utils.http.{CommonHttpHeader, ServiceClientResponse}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Handles all of the API interactions necessary for the [[SamlPolicyTranslationFilter]].
  *
  * The default singleton scope is used, and works, due entirely to the separation of
  * Spring contexts between filters.
  */
@Named
class SamlPolicyProvider @Inject()(akkaServiceClientFactory: AkkaServiceClientFactory) {

  import SamlPolicyProvider._

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
      optTokenServiceClient.foreach(_.akkaServiceClient.destroy())
      tokenServiceClient = ServiceClient(
        tokenPoolId,
        tokenPoolId.map(akkaServiceClientFactory.newAkkaServiceClient)
          .getOrElse(akkaServiceClientFactory.newAkkaServiceClient)
      )
    }

    val optPolicyServiceClient = Option(policyServiceClient)
    if (optPolicyServiceClient.isEmpty || policyServiceClient.poolId != policyPoolId) {
      optPolicyServiceClient.foreach(_.akkaServiceClient.destroy())
      policyServiceClient = ServiceClient(
        policyPoolId,
        policyPoolId.map(akkaServiceClientFactory.newAkkaServiceClient)
          .getOrElse(akkaServiceClientFactory.newAkkaServiceClient)
      )
    }
  }

  /**
    * Retrieves a token from Identity that will be used for authorization on all other calls to Identity.
    *
    * @param username the username on the account
    * @param password the password on the account
    * @param traceId an optional identifier to be sent with the request
    * @param checkCache whether or not to use the HTTP request cache
    * @return the token if successful, or a failure if unsuccessful
    */
  def getToken(username: String, password: String, traceId: Option[String], checkCache: Boolean = true): Try[String] = {
    val authenticationPayload = Json.obj(
      "auth" -> Json.obj(
        "passwordCredentials" -> Json.obj(
          "username" -> username,
          "password" -> password
        )
      )
    )

    val akkaResponse = Try(tokenServiceClient.akkaServiceClient.post(
      TokenRequestKey,
      s"$tokenUri$TokenPath",
      (Map(CommonHttpHeader.ACCEPT.toString -> MediaType.APPLICATION_JSON)
        ++ traceId.map(CommonHttpHeader.TRACE_GUID.toString -> _)).asJava,
      Json.stringify(authenticationPayload),
      MediaType.APPLICATION_JSON_TYPE,
      checkCache
    ))

    akkaResponse match {
      case Success(serviceClientResponse) =>
        serviceClientResponse.getStatus match {
          case SC_OK =>
            Try {
              val responseEncoding = serviceClientResponse.getHeaders
                .find(hdr => CommonHttpHeader.CONTENT_TYPE.matches(hdr.getName))
                .map(_.getElements.head.getParameterByName("charset"))
                .flatMap(Option.apply)
                .map(_.getValue)
                .getOrElse(StandardCharsets.ISO_8859_1.name())
              val jsonResponse = Source.fromInputStream(serviceClientResponse.getData, responseEncoding).getLines.mkString
              val json = Json.parse(jsonResponse)
              (json \ "access" \ "token" \ "id").as[String]
            } recover {
              case f: Exception =>
                throw GenericIdentityException("Token could not be parsed from response from Identity", f)
            }
          case SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS =>
            Failure(OverLimitException(buildRetryValue(serviceClientResponse), "Rate limited when getting token"))
          case statusCode =>
            Failure(UnexpectedStatusCodeException(statusCode, "Unexpected response from Identity"))
        }
      case Failure(f) =>
        Failure(GenericIdentityException("Failure communicating with Identity when getting token", f))
    }
  }

  /**
    * Retrieves a policy from Identity that will be used during SAMLResponse translation.
    *
    * @param token the token to be sent with the request, used in authorization
    * @param traceId an optional identifier to be sent with the request
    * @return the policy if successful, or a failure if unsuccessful
    */
  def getPolicy(traceId: Option[String]): Try[String] = {
    ???
  }

  private case class ServiceClient(poolId: Option[String], akkaServiceClient: AkkaServiceClient)

}

object SamlPolicyProvider {
  final val SC_TOO_MANY_REQUESTS = 429
  final val TokenRequestKey = "SAML:TOKEN"
  final val TokenPath = "/v2.0/tokens"

  def buildRetryValue(response: ServiceClientResponse): String = {
    response.getHeaders
      .find(hdr => CommonHttpHeader.RETRY_AFTER.matches(hdr.getName))
      .map(_.getValue)
      .getOrElse(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")).plusSeconds(5)))
  }

  trait IdentityException extends Exception

  case class GenericIdentityException(message: String, cause: Throwable)
    extends Exception(message, cause) with IdentityException

  case class UnexpectedStatusCodeException(statusCode: Int, message: String)
    extends Exception(message)

  case class OverLimitException(retryAfter: String, message: String)
    extends Exception(message) with IdentityException

}
