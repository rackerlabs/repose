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
package org.openrepose.filters.keystonev2basicauth

import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.{Calendar, GregorianCalendar}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.HttpHeaders
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.http.{CommonHttpHeader, HttpDate}
import org.openrepose.commons.utils.servlet.filter.FilterAction
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.openrepose.filters.keystonev2basicauth.config.{KeystoneV2BasicAuthConfig, SecretType}

import scala.Function.tupled
import scala.util.Try
import scala.xml.XML

@Named
class KeystoneV2BasicAuthFilter @Inject()(configurationService: ConfigurationService,
                                          httpClientService: HttpClientService,
                                          datastoreService: DatastoreService)
  extends Filter
    with UpdateListener[KeystoneV2BasicAuthConfig]
    with HttpDelegationManager
    with BasicAuthUtils
    with StrictLogging {

  private final val DEFAULT_CONFIG = "keystone-v2-basic-auth.cfg.xml"
  private final val TOKEN_ENDPOINT = "/v2.0/tokens"
  private final val TOKEN_KEY_PREFIX = "TOKEN:"
  private final val X_AUTH_TOKEN = "X-Auth-Token"
  private final val SC_TOO_MANY_REQUESTS = 429
  private val datastore = datastoreService.getDefaultDatastore
  private var initialized = false
  private var config: String = _
  private var identityServiceUri: String = _
  private var tokenCacheTtlMillis: Int = _
  private var delegationWithQuality: Option[Double] = _
  private var secretType: SecretType = _
  private var httpClient: HttpClientServiceClient = _

  override def init(filterConfig: FilterConfig) {
    config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + config)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/keystone-v2-basic-auth.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      config,
      xsdURL,
      this,
      classOf[KeystoneV2BasicAuthConfig]
    )
    logger.warn("WARNING: This filter cannot be used alone, it requires an AuthFilter after it.")
  }

  override def configurationUpdated(config: KeystoneV2BasicAuthConfig) {
    identityServiceUri = config.getKeystoneV2ServiceUri
    tokenCacheTtlMillis = config.getTokenCacheTimeoutMillis
    delegationWithQuality = Option(config.getDelegating).map(_.getQuality)
    secretType = config.getSecretType

    httpClient = httpClientService.getClient(config.getConnectionPoolId)

    initialized = true
  }

  override def isInitialized = {
    initialized
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val wrappedHttpRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val httpResponse = servletResponse.asInstanceOf[HttpServletResponse]

      val filterAction = handleRequest(wrappedHttpRequest, httpResponse)
      filterAction match {
        case FilterAction.RETURN => // no action to take
        case FilterAction.PROCESS_RESPONSE =>
          filterChain.doFilter(wrappedHttpRequest, httpResponse)
          handleResponse(wrappedHttpRequest, httpResponse)
        case _ =>
          logger.error("Unexpected internal filter state (FilterAction)")
          httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }

  def handleRequest(httpServletRequestWrapper: HttpServletRequestWrapper, httpServletResponse: HttpServletResponse): FilterAction = {
    logger.debug("Handling HTTP Request")
    var filterAction = FilterAction.PROCESS_RESPONSE

    def delegateOrElse(responseCode: Int, message: String)(f: => Any) = {
      delegationWithQuality match {
        case Some(quality) =>
          buildDelegationHeaders(responseCode, "Keystone v2 Basic Auth", message, quality) foreach { case (key, values) =>
            values foreach { value =>
              httpServletRequestWrapper.addHeader(key, value)
            }
          }
        case None =>
          f
      }
    }

    def processFailedToken(tokenResponseInfo: TokenCreationInfo, encodedCredentials: String): Unit = {
      import HttpServletResponse._
      tokenResponseInfo match {
        case TokenCreationInfo(SC_UNAUTHORIZED, _, userName, _, _) =>
          delegateOrElse(SC_UNAUTHORIZED, s"Failed to authenticate user: $userName") {
            httpServletResponse.setStatus(SC_UNAUTHORIZED) // (401)
            httpServletResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, """Basic realm="RAX-KEY"""")
            datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
          }
        case TokenCreationInfo((SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS), _, userName, Some(retry), _) => // (413 | 429)
          delegateOrElse(SC_SERVICE_UNAVAILABLE, "Rate limited by identity service") {
            httpServletResponse.setStatus(SC_SERVICE_UNAVAILABLE) // (503)
            httpServletResponse.addHeader(HttpHeaders.RETRY_AFTER, retry)
          }
        case TokenCreationInfo(SC_BAD_REQUEST, _, userName, _, identityResponseBody) =>
          identityResponseBody.foreach { responseContent =>
            logger.warn(s"Bad Request received from identity for $userName. Identity Response: $responseContent")
          }
          delegateOrElse(SC_UNAUTHORIZED, s"Bad Request received from identity service for $userName") {
            httpServletResponse.setStatus(SC_UNAUTHORIZED)
            httpServletResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, """Basic realm="RAX-KEY"""")
            datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
          }
        case TokenCreationInfo(SC_FORBIDDEN, _, userName, _, _) =>
          delegateOrElse(SC_FORBIDDEN, s"$userName is forbidden") {
            httpServletResponse.setStatus(SC_FORBIDDEN) // (401)
            //Not removing it from the datastore, they're forbidden, cache is legit
          }
        case TokenCreationInfo(SC_NOT_FOUND, _, userName, _, _) =>
          logger.warn(s"404 Received from identity attempting to authenticate $userName")

          delegateOrElse(SC_UNAUTHORIZED, s"Failed to authenticate user: $userName") {
            httpServletResponse.setStatus(SC_UNAUTHORIZED) // (401)
            httpServletResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, """Basic realm="RAX-KEY"""")
            datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
          }
        case (_) =>
          delegateOrElse(SC_INTERNAL_SERVER_ERROR, "Failed with internal server error") {
            httpServletResponse.setStatus(SC_INTERNAL_SERVER_ERROR) // (500)
          }
      }
      if (delegationWithQuality.isEmpty) filterAction = FilterAction.RETURN
    }

    def getUserToken(authValue: String): TokenCreationInfo = {
      val (userName, secret) = extractCredentials(authValue)

      def createAuthRequest(encoded: String) = {
        // Base64 Decode and split the username/secret
        // Scala's standard XML syntax does not support the XML declaration w/o a lot of hoops
        //<?xml version="1.0" encoding="UTF-8"?>
        secretType match {
          case SecretType.API_KEY =>
            <auth xmlns="http://docs.openstack.org/identity/api/v2.0">
              <apiKeyCredentials
              xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
              username={userName}
              apiKey={secret}/>
            </auth>
          case SecretType.PASSWORD =>
            <auth xmlns="http://docs.openstack.org/identity/api/v2.0">
              <passwordCredentials
              xmlns="http://docs.openstack.org/identity/api/v2.0"
              username={userName}
              password={secret}/>
            </auth>
        }
      }

      if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(secret)) {
        TokenCreationInfo(HttpServletResponse.SC_UNAUTHORIZED, None, userName)
      } else {
        // Request a User Token based on the extracted username/secret
        val requestTracingHeader = Option(httpServletRequestWrapper.getHeader(CommonHttpHeader.TRACE_GUID))
          .map(guid => Map(CommonHttpHeader.TRACE_GUID -> guid)).getOrElse(Map.empty)

        val requestBody = EntityBuilder.create()
          .setText(createAuthRequest(authValue).toString())
          .setContentType(ContentType.APPLICATION_XML)
          .build()
        val requestBuilder = RequestBuilder.post(identityServiceUri + TOKEN_ENDPOINT)
          .setEntity(requestBody)
        requestTracingHeader.foreach(tupled(requestBuilder.addHeader))
        val request = requestBuilder.build()

        val cachingContext = CachingHttpClientContext.create()
          .setCacheKey(authValue)

        val tryResponse = Try(httpClient.execute(request, cachingContext))

        tryResponse.map { response =>
          val responseBody = EntityUtils.toString(response.getEntity)
          response.getStatusLine.getStatusCode match {
            case HttpServletResponse.SC_OK =>
              val xmlString = XML.loadString(responseBody)
              val idString = (xmlString \\ "access" \ "token" \ "@id").text
              TokenCreationInfo(response.getStatusLine.getStatusCode, Option(idString), userName)
            case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE | SC_TOO_MANY_REQUESTS =>
              val retryHeaders = response.getHeaders(HttpHeaders.RETRY_AFTER).headOption.map(_.getValue).orElse {
                logger.info(s"Missing ${HttpHeaders.RETRY_AFTER} header on Auth Response status code: ${response.getStatusLine.getStatusCode}")
                val retryCalendar = new GregorianCalendar()
                retryCalendar.add(Calendar.SECOND, 5)
                Option(new HttpDate(retryCalendar.getTime).toRFC1123)
              }
              TokenCreationInfo(response.getStatusLine.getStatusCode, None, userName, retryHeaders, Some(responseBody))
            case _ =>
              TokenCreationInfo(response.getStatusLine.getStatusCode, None, userName, None, Some(responseBody))
          }
        }.getOrElse {
          TokenCreationInfo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, None, userName)
        }
      }
    }

    def processNoCachedToken(encodedCredentials: String): Unit = {
      // request a token
      getUserToken(encodedCredentials) match {
        case TokenCreationInfo(_, Some(token), _, _, _) =>
          val tokenStr = token.toString
          if (tokenCacheTtlMillis > 0) {
            datastore.put(TOKEN_KEY_PREFIX + encodedCredentials, tokenStr, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
          }
          httpServletRequestWrapper.addHeader(X_AUTH_TOKEN, tokenStr)
        case failure: TokenCreationInfo =>
          processFailedToken(failure, encodedCredentials)
      }
    }

    // We need to process the Response unless a couple of specific conditions occur.
    if (!httpServletRequestWrapper.getHeaderNamesScala.contains(X_AUTH_TOKEN)) {
      withEncodedCredentials(httpServletRequestWrapper) { encodedCredentials =>
        Option(datastore.get(TOKEN_KEY_PREFIX + encodedCredentials)) match {
          case Some(token) =>
            val tokenString = token.toString
            httpServletRequestWrapper.addHeader(X_AUTH_TOKEN, tokenString)
          case None =>
            processNoCachedToken(encodedCredentials)
        }
      }
    }

    filterAction
  }

  def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Unit = {
    val responseStatus = httpServletResponse.getStatus
    logger.debug("Handling HTTP Response. Incoming status code: " + responseStatus)
    if (responseStatus == HttpServletResponse.SC_UNAUTHORIZED ||
      responseStatus == HttpServletResponse.SC_FORBIDDEN) {
      httpServletResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, """Basic realm="RAX-KEY"""")
      withEncodedCredentials(httpServletRequest) { encodedCredentials =>
        datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
      }
    }
    logger.debug("Keystone v2 Basic Auth Response. Outgoing status code: " + httpServletResponse.getStatus)
  }

  private def withEncodedCredentials(request: HttpServletRequest)(f: String => Unit): Unit = {
    Option(request.getHeaders(HttpHeaders.AUTHORIZATION)) foreach { authHeader =>
      val authMethodBasicHeaders = getBasicAuthHeaders(authHeader, "Basic")
      if (authMethodBasicHeaders.nonEmpty) {
        val firstHeader = authMethodBasicHeaders.next()
        f(firstHeader.replace("Basic", "").trim)
      }
    }
  }

  override def destroy() {
    configurationService.unsubscribeFrom(config, this.asInstanceOf[UpdateListener[_]])
  }

  /**
   * Token creation info encapsulates the response from identity.
   *
   * @param responseCode Response code received from identity
   * @param responseBody Optional Response body from identity if it's not successful
   * @param userId The user ID parsed from a successful response (if any)
   * @param userName
   * @param retry
   */
  case class TokenCreationInfo(responseCode: Int, userId: Option[String], userName: String, retry: Option[String] = None, responseBody: Option[String] = None)

}
