package org.openrepose.filters.rackspaceidentitybasicauth

import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.{HttpHeaders, MediaType}

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.filters.rackspaceidentitybasicauth.config.RackspaceIdentityBasicAuthConfig
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.filter.logic.{FilterAction, FilterDirector}
import org.openrepose.services.datastore.DatastoreService
import org.openrepose.services.serviceclient.akka.AkkaServiceClient
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.io.Source
import scala.xml._

class RackspaceIdentityBasicAuthHandler(basicAuthConfig: RackspaceIdentityBasicAuthConfig, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[RackspaceIdentityBasicAuthHandler])
  private final val TOKEN_KEY_PREFIX = "TOKEN:"
  private final val X_AUTH_TOKEN = "X-Auth-Token"
  private val identityServiceUri = basicAuthConfig.getRackspaceIdentityServiceUri
  private val tokenCacheTtlMillis = basicAuthConfig.getTokenCacheTimeoutMillis
  private val datastore = datastoreService.getDefaultDatastore

  override def handleRequest(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Request")
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // We need to process the Response unless a couple of specific conditions occur.
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    if (!httpServletRequest.getHeaderNames.asScala.toList.contains(X_AUTH_TOKEN)) {
      withEncodedCredentials(httpServletRequest) { encodedCredentials =>
        Option(datastore.get(TOKEN_KEY_PREFIX + encodedCredentials)) match {
          case Some(token) => {
            val tokenString = token.toString()
            filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, tokenString)
          }
          case None => {
            // request a token
            getUserToken(encodedCredentials) match {
              case (code, Some(token)) => {
                val tokenStr = token.toString()
                if (tokenCacheTtlMillis > 0) {
                  datastore.put(TOKEN_KEY_PREFIX + encodedCredentials, tokenStr, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
                }
                filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, tokenStr)
              }
              case (code, _) => {
                if (code == HttpServletResponse.SC_UNAUTHORIZED) {
                  filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED) // (401)
                  filterDirector.responseHeaderManager().appendHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"RAX-KEY\"")
                  datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
                } else {
                  filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR) // (500)
                }
                filterDirector.setFilterAction(FilterAction.RETURN)
              }
            }
          }
        }
      }
    }
    filterDirector
  }

  override def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Response. Incoming status code: " + httpServletResponse.getStatus())
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    if (httpServletResponse.getStatus == HttpServletResponse.SC_UNAUTHORIZED ||
      httpServletResponse.getStatus == HttpServletResponse.SC_FORBIDDEN) {
      filterDirector.responseHeaderManager().appendHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"RAX-KEY\"")
      withEncodedCredentials(httpServletRequest) { encodedCredentials =>
        datastore.remove(TOKEN_KEY_PREFIX + encodedCredentials)
      }
    }
    LOG.debug("Rackspace Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }

  private def withEncodedCredentials(request: HttpServletRequest)(f: String => Unit): Unit = {
    Option(request.getHeaders(HttpHeaders.AUTHORIZATION)).map { authHeader =>
      val authMethodBasicHeaders = BasicAuthUtils.getBasicAuthHeaders(authHeader, "Basic")
      if (authMethodBasicHeaders.nonEmpty) {
        val firstHeader = authMethodBasicHeaders.next()
        f(firstHeader.replace("Basic ", ""))
      }
    }
  }

  private def getUserToken(authValue: String): (Int, Option[String]) = {
    def createAuthRequest(encoded: String) = {
      // Base64 Decode and split the userName/apiKey
      val (userName, apiKey) = BasicAuthUtils.extractCredentials(authValue)
      // Scala's standard XML syntax does not support the XML declaration w/o a lot of hoops
      //<?xml version="1.0" encoding="UTF-8"?>
      <auth xmlns="http://docs.openstack.org/identity/api/v2.0">
        <apiKeyCredentials
        xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
        username={userName}
        apiKey={apiKey}/>
      </auth>
    }
    // Request a User Token based on the extracted User Name/API Key.
    val authTokenResponse = Option(akkaServiceClient.post(authValue,
      identityServiceUri,
      Map[String, String]().asJava,
      createAuthRequest(authValue).toString,
      MediaType.APPLICATION_XML_TYPE))

    authTokenResponse.map { tokenResponse =>
      val statusCode = tokenResponse.getStatusCode
      if (statusCode == HttpServletResponse.SC_OK) {
        val xmlString = XML.loadString(Source.fromInputStream(tokenResponse.getData()).mkString)
        val idString = (xmlString \\ "access" \ "token" \ "@id").text
        (statusCode, Option(idString))
      } else {
        (statusCode, None)
      }
    } getOrElse {
      (HttpServletResponse.SC_INTERNAL_SERVER_ERROR, None)
    }
  }
}
