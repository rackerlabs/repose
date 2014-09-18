package com.rackspace.papi.components.openstack.identity.basicauth

import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.openstack.identity.basicauth.config.OpenStackIdentityBasicAuthConfig
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.parsing.json._

class OpenStackIdentityBasicAuthHandler(basicAuthConfig: OpenStackIdentityBasicAuthConfig, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[OpenStackIdentityBasicAuthHandler])
  private final val TOKEN_KEY_PREFIX = "TOKEN:"
  private final val X_AUTH_TOKEN = "X-Auth-Token"
  private val identityServiceUri = basicAuthConfig.getOpenstackIdentityServiceUri
  private val tokenCacheTtlMillis = basicAuthConfig.getTokenCacheTimeoutMillis
  private val datastore = datastoreService.getDefaultDatastore

  override def handleRequest(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Request")
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // IF request has a HTTP Basic authentication header (Authorization) with method of Basic, THEN ...; ELSE ...
    val optionHeaders = Option(httpServletRequest.getHeaders(HttpHeaders.AUTHORIZATION))
    val authMethodBasicHeaders = BasicAuthUtils.getBasicAuthHdrs(optionHeaders, "Basic")
    var tokenFound = false
    if (authMethodBasicHeaders.isDefined && !(authMethodBasicHeaders.get.isEmpty)) {
      // FOR EACH HTTP Basic authentication header (Authorization) with method Basic...
      for (authHeader <- authMethodBasicHeaders.get) {
        val authValue = authHeader.replace("Basic ", "")
        var token = Option(datastore.get(TOKEN_KEY_PREFIX + authValue))
        // IF the userName/apiKey is in the cache,
        // THEN add the token header;
        // ELSE ...
        if (token.isDefined) {
          filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
          tokenFound = true
        } else {
          // request a token
          token = getUserToken(authValue)
          // IF a token was received, THEN ...
          if (token.isDefined) {
            // add the token header
            filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
            // cache the token with the configured cache timeout
            datastore.put(TOKEN_KEY_PREFIX + authValue, token.get.toString, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
            tokenFound = true
          }
        }
      }
    }
    if (!tokenFound) {
      // set the response status code to UNAUTHORIZED (401)
      filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED)
      //// set the response status code to FORBIDDEN (403)
      //filterDirector.setResponseStatusCode(HttpServletResponse.SC_FORBIDDEN)
    }
    // No matter what, we need to process the response.
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    filterDirector
  }

  private def getUserToken(authValue: String): Option[String] = {
    val createJsonAuthRequest = (encoded: String) => {
      val (userName, apiKey) = BasicAuthUtils.extractCreds(authValue)
      s"""
      |{
      |  "auth": {
      |    "RAX-KSKEY:apiKeyCredentials": {
      |      "username": "${userName}",
      |      "apiKey": "${apiKey}"
      |    }
      |  }
      |}
      """.stripMargin
    }
    // Base64 Decode and split the userName/apiKey
    val authTokenResponse = Option(akkaServiceClient.post(authValue,
      identityServiceUri + "/v2.0/tokens",
      Map[String, String]().asJava,
      createJsonAuthRequest(authValue),
      MediaType.APPLICATION_JSON_TYPE,
      MediaType.APPLICATION_JSON_TYPE))

    if (authTokenResponse.isDefined) {
      authTokenResponse.get.getStatusCode match {
        // Since the operation is a POST, an OK (200) for V2 or CREATED (201) for V3 should be returned if the operation was successful
        case HttpServletResponse.SC_OK | HttpServletResponse.SC_CREATED =>
          def authRespDataRaw = authTokenResponse.get.getData
          def authRespDataStr = Source.fromInputStream(authRespDataRaw).mkString
          def authRespDataJson = JSON.parseFull(authRespDataStr)
          //authRespDataJson.map(_("access")("token")("id")).getOrElse(None)
          def authRespDataJsonMap = authRespDataJson.get
          LOG.debug(authRespDataJsonMap.toString())
          //Option("TODO: RETRIEVE THIS")
          Option("this-is-the-token")
        case _ =>
          None
      }
    }
    else {
      None
    }
  }

  override def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Response. Incoming status code: " + httpServletResponse.getStatus())
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // IF response Status Code is UNAUTHORIZED (401) OR FORBIDDEN (403), THEN
    if (httpServletResponse.getStatus == HttpServletResponse.SC_UNAUTHORIZED ||
      httpServletResponse.getStatus == HttpServletResponse.SC_FORBIDDEN) {
      // add HTTP Basic authentication header (WWW-Authenticate) with the realm of RAX-KEY
      filterDirector.responseHeaderManager().appendHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"RAX-KEY\"")
      // IF request has a HTTP Basic authentication header (Authorization),
      // THEN remove the encoded userName/apiKey (Key) & token (Value) cached in the Datastore
      val optionHeaders = Option(httpServletRequest.getHeaders(HttpHeaders.AUTHORIZATION))
      val authMethodBasicHeaders = BasicAuthUtils.getBasicAuthHdrs(optionHeaders, "Basic")
      if (authMethodBasicHeaders.isDefined && !(authMethodBasicHeaders.get.isEmpty)) {
        for (authHeader <- authMethodBasicHeaders.get) {
          val authValue = authHeader.replace("Basic ", "")
          datastore.remove(X_AUTH_TOKEN + authValue)
        }
      }
    }
    LOG.debug("OpenStack Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }
}
