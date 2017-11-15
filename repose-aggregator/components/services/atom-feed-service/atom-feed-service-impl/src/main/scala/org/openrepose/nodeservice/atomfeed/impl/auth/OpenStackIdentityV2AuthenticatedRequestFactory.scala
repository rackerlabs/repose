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
package org.openrepose.nodeservice.atomfeed.impl.auth

import java.util
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.servlet.http.HttpServletResponse._
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.{AuthenticatedRequestFactory, AuthenticationRequestContext, AuthenticationRequestException, FeedReadRequest}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

object OpenStackIdentityV2AuthenticatedRequestFactory {
  private final val TOKENS_ENDPOINT = "/v2.0/tokens"
  private final val AKKA_HASH_KEY = OpenStackIdentityV2AuthenticatedRequestFactory.getClass.getName
}

/**
  * Fetches a token from the OpenStack Identity service, if necessary, then adds the token to the request.
  */
class OpenStackIdentityV2AuthenticatedRequestFactory @Inject()(feedConfig: AtomFeedConfigType,
                                                               authConfig: OpenStackIdentityV2AuthenticationType,
                                                               akkaServiceClientFactory: AkkaServiceClientFactory)
  extends AuthenticatedRequestFactory with LazyLogging {

  import OpenStackIdentityV2AuthenticatedRequestFactory._

  private val serviceUri = authConfig.getUri
  private val username = authConfig.getUsername
  private val password = authConfig.getPassword

  private var cachedToken: Option[String] = None
  private var akkaServiceClient: AkkaServiceClient = akkaServiceClientFactory.newAkkaServiceClient(feedConfig.getConnectionPoolId)

  override def authenticateRequest(feedReadRequest: FeedReadRequest, context: AuthenticationRequestContext): FeedReadRequest = {
    lazy val tracingHeader = TracingHeaderHelper.createTracingHeader(context.getRequestId, "1.1 Repose (Repose/" + context.getReposeVersion + ")", username)

    val tryToken = cachedToken match {
      case Some(tkn) => Success(tkn)
      case None => getToken(tracingHeader)
    }

    tryToken match {
      case Success(tkn) =>
        logger.debug(s"Adding x-auth-token header with value: $tkn")
        feedReadRequest.getHeaders.put(CommonHttpHeader.AUTH_TOKEN, util.Arrays.asList(tkn))
        feedReadRequest
      case Failure(ex) =>
        throw new AuthenticationRequestException("Failed to authenticate the request.", ex)
    }
  }

  private def getToken(tracingHeader: String): Try[String] = {
    logger.debug("Attempting to get token from Identity")

    val akkaResponse = Try(akkaServiceClient.post(
      AKKA_HASH_KEY,
      s"$serviceUri$TOKENS_ENDPOINT",
      Map(HttpHeaders.ACCEPT -> MediaType.APPLICATION_JSON,
        CommonHttpHeader.TRACE_GUID -> tracingHeader).asJava,
      Json.stringify(Json.obj(
        "auth" -> Json.obj(
          "passwordCredentials" -> Json.obj(
            "username" -> username,
            "password" -> password)))),
      MediaType.APPLICATION_JSON_TYPE
    ))

    akkaResponse match {
      case Success(serviceClientResponse) if Option(serviceClientResponse).isDefined =>
        serviceClientResponse.getStatus match {
          case statusCode@(SC_OK | SC_NON_AUTHORITATIVE_INFORMATION) =>
            val jsonResponse = Source.fromInputStream(serviceClientResponse.getData).getLines().mkString("")
            Try(Json.parse(jsonResponse)) match {
              case Success(json) =>
                Try(Success((json \ "access" \ "token" \ "id").as[String])) match {
                  case Success(token) =>
                    logger.debug("Successfully fetched and parsed token from identity service")
                    cachedToken = Some(token.get)
                    token
                  case Failure(f) =>
                    logger.error(s"Response from the identity service was not in JSON format as expected: ${f.getLocalizedMessage}")
                    Failure(new Exception("Response from the identity service was not in JSON format as expected", f))
                }
              case Failure(f) =>
                logger.error(s"Response from the identity service was not in JSON format as expected: ${f.getLocalizedMessage}")
                Failure(new Exception("Response from the identity service was not in JSON format as expected", f))
            }
          case statusCode =>
            logger.error(s"Failed to retrieve token from the identity service, status code: $statusCode")
            Failure(new Exception(s"Failed to retrieve token from the identity service, status code: $statusCode"))
        }
      case Failure(f) =>
        logger.error(s"Failed to retrieve token from the identity service due to: ${f.getLocalizedMessage}")
        Failure(new Exception("Failed to retrieve token from the identity service.", f))
      case _ =>
        logger.error("Failed to retrieve token from the identity service.")
        Failure(new Exception("Failed to retrieve token from the identity service."))
    }
  }

  override def onInvalidCredentials(): Unit = cachedToken = None

  @PreDestroy
  def destroy(): Unit = {
    Option(akkaServiceClient).foreach(_.destroy())
  }
}
