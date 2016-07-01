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

import java.io.InputStream
import java.util

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpHeaders, HttpStatus}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.docs.repose.atom_feed_service.v1.OpenStackIdentityV2AuthenticationType
import org.openrepose.nodeservice.atomfeed.{AuthenticatedRequestFactory, AuthenticationRequestContext, FeedReadRequest}
import play.api.libs.json.Json

import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

object OpenStackIdentityV2AuthenticatedRequestFactory {
  private final val TOKENS_ENDPOINT = "/v2.0/tokens"
}

/**
  * Fetches a token from the OpenStack Identity service, if necessary, then adds the token to the request.
  */
class OpenStackIdentityV2AuthenticatedRequestFactory(configuration: OpenStackIdentityV2AuthenticationType)
  extends AuthenticatedRequestFactory with LazyLogging {

  import OpenStackIdentityV2AuthenticatedRequestFactory._

  private val serviceUri = configuration.getUri
  private val username = configuration.getUsername
  private val password = configuration.getPassword
  private val httpClient = HttpClients.createDefault()

  private var cachedToken: Option[String] = None

  override def authenticateRequest(feedReadRequest: FeedReadRequest, context: AuthenticationRequestContext): FeedReadRequest = {
    lazy val tracingHeader = TracingHeaderHelper.createTracingHeader(context.getRequestId, "1.1 Repose (Repose/" + context.getReposeVersion + ")", username)

    val tryToken = cachedToken match {
      case Some(tkn) => Success(tkn)
      case None => getToken(tracingHeader)
    }

    tryToken match {
      case Success(tkn) =>
        logger.debug(s"Adding x-auth-token header with value: $tkn")
        feedReadRequest.getHeaders.put(CommonHttpHeader.AUTH_TOKEN.toString, util.Arrays.asList(tkn))
        feedReadRequest
      case Failure(_) =>
        null
    }
  }

  private def getToken(tracingHeader: String): Try[String] = {
    logger.debug("Attempting to get token from Identity")

    val httpPost = new HttpPost(s"$serviceUri$TOKENS_ENDPOINT")
    val requestBody = Json.stringify(Json.obj(
      "auth" -> Json.obj(
        "passwordCredentials" -> Json.obj(
          "username" -> username,
          "password" -> password))))
    httpPost.addHeader(CommonHttpHeader.TRACE_GUID.toString, tracingHeader)
    httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType)
    httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON))

    val httpResponse = httpClient.execute(httpPost)

    try {
      httpResponse.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK | HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION =>
          val responseEntity = httpResponse.getEntity

          if (ContentType.APPLICATION_JSON.getMimeType.equalsIgnoreCase(responseEntity.getContentType.getValue)) {
            val token = parseTokenFromJson(responseEntity.getContent)

            logger.debug("Successfully fetched and parsed token from identity service")
            cachedToken = Some(token)

            Success(token)
          } else {
            logger.error("Response from the identity service was not in JSON format as expected")
            Failure(new Exception("Response from the identity service was not in JSON format as expected"))
          }
        case statusCode =>
          logger.error(s"Failed to retrieve token from the identity service, status code: $statusCode")
          Failure(new Exception(s"Failed to retrieve token from the identity service, status code: $statusCode"))
      }
    } finally {
      EntityUtils.consume(httpResponse.getEntity)
      httpResponse.close()
    }
  }

  private def parseTokenFromJson(tokenStream: InputStream): String = {
    val contentString = Source.fromInputStream(tokenStream)(Codec.UTF8).mkString

    (Json.parse(contentString) \ "access" \ "token" \ "id").as[String]
  }

  override def onInvalidCredentials(): Unit = cachedToken = None
}
