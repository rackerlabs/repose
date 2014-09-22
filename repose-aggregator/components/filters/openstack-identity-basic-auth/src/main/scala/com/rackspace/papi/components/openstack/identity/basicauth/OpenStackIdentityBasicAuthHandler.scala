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
import scala.xml._

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
    // We need to process the Response unless a couple of specific conditions occur.
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    // IF request already has an X-Auth-Token header,
    // THEN skip any HTTP Basic authentication headers (Authorization) that may be present.
    val optionXAuthHeaders = Option(httpServletRequest.getHeaders(X_AUTH_TOKEN))
    if (!optionXAuthHeaders.isDefined || !(optionXAuthHeaders.get.hasMoreElements)) {
      // IF request has a HTTP Basic authentication header (Authorization) with method of Basic, THEN ...
      val optionHeaders = Option(httpServletRequest.getHeaders(HttpHeaders.AUTHORIZATION))
      val authMethodBasicHeaders = BasicAuthUtils.getBasicAuthHdrs(optionHeaders, "Basic")
      if (authMethodBasicHeaders.isDefined && !(authMethodBasicHeaders.get.isEmpty)) {
        // FOR EACH HTTP Basic authentication header (Authorization) with method Basic...
        var tokenFound = false
        var code = HttpServletResponse.SC_OK
        // THIS First in wins
        val authHeader = authMethodBasicHeaders.get.next()
        // OR Loop through until we find a good one.
        //for (authHeader <- authMethodBasicHeaders.get.next()
        //if !tokenFound) {
          val authValue = authHeader.replace("Basic ", "")
          var token = Option(datastore.get(TOKEN_KEY_PREFIX + authValue))
          // IF the userName/apiKey is in the cache,
          // THEN add the token header;
          // ELSE ...
          if (token.isDefined && !(token.isEmpty)) {
            val tokenStr = token.get.toString
            if (tokenStr.length > 0) {
              filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
              tokenFound = true
            }
          } else {
            // request a token
            val rtn = getUserToken(authValue)
            code = rtn._1
            token = rtn._2
            // IF a token was received, THEN ...
            if (token.isDefined && !(token.isEmpty)) {
              val tokenStr = token.get.toString
              if (tokenStr.length > 0) {
                // add the token header
                filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
                // cache the token with the configured cache timeout
                datastore.put(TOKEN_KEY_PREFIX + authValue, token.get.toString, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
                tokenFound = true
              }
            }
          }
        //}
        // IF the Token was not found, THEN ...
        if (!tokenFound) {
          // IF the status code from the Identity Service is UNAUTHORIZED (401) or NOT_FOUND (404)
          // THEN set the response, add the header, and return;
          // ELSE set the response to INTERNAL_SERVER_ERROR (500) and return.
          code match {
            case HttpServletResponse.SC_UNAUTHORIZED |                // (401)
                 HttpServletResponse.SC_NOT_FOUND =>                  // (404)
            handleUnauthorized(filterDirector, httpServletRequest)
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED)           // (401)
            case _ =>
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)  // (500)
          }
          filterDirector.setFilterAction(FilterAction.RETURN)
        }
      }
    }
    ////////////////////////////////////////////////////////////////////////////////
    // TODO: This is required for Deproxy MessageChain.orphanedHandlings() to stabilize before the Deproxy.makeRequest() returns.
    Thread sleep 500 // All Pass
    //Thread sleep 250 // Every other one Passes
    //Thread sleep 125 // First one passes
    ////////////////////////////////////////////////////////////////////////////////
    filterDirector
  }

  private def getUserToken(authValue: String): (Int, Option[String]) = {
    val createAuthRequest = (encoded: String) => {
      // Base64 Decode and split the userName/apiKey
      val (userName, apiKey) = BasicAuthUtils.extractCreds(authValue)
      // Scala's standard XML syntax does not support the XML declaration w/o a lot of hoops
      //<?xml version="1.0" encoding="UTF-8"?>
      <auth xmlns="http://docs.openstack.org/identity/api/v2.0">
        <apiKeyCredentials
          xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
          username={ userName }
          apiKey={ apiKey }/>
      </auth>
    }
    // Request a User Token based on the extracted User Name/API Key.
    val authTokenResponse = Option(akkaServiceClient.post(authValue,
      identityServiceUri,
      Map[String, String]().asJava,
      createAuthRequest(authValue).toString,
      MediaType.APPLICATION_XML_TYPE))

    // IF the Akka Service Client gives a response, THEN...;
    // ELSE just return INTERNAL_SERVER_ERROR (500).
    if (authTokenResponse.isDefined) {
      val statusCode = authTokenResponse.get.getStatusCode
      // IF the response is OK (200),
      // THEN extract the Token;
      // ELSE just return the code.
      if (statusCode == HttpServletResponse.SC_OK) {
        def authRespDataRaw = authTokenResponse.get.getData
        def authRespDataStr = Source.fromInputStream(authRespDataRaw).mkString
        def xmlString = XML.loadString(authRespDataStr)
        val idString = (xmlString \\ "access" \ "token" \ "@id").text
        (statusCode, Option(idString))
      } else {
        (statusCode, None)
      }
    }
    else {
        (HttpServletResponse.SC_INTERNAL_SERVER_ERROR, None)
    }
  }

  override def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Response. Incoming status code: " + httpServletResponse.getStatus())
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // IF response Status Code is UNAUTHORIZED (401) OR FORBIDDEN (403), THEN
    if (httpServletResponse.getStatus == HttpServletResponse.SC_UNAUTHORIZED ||
      httpServletResponse.getStatus == HttpServletResponse.SC_FORBIDDEN) {
      handleUnauthorized(filterDirector, httpServletRequest)
    }
    LOG.debug("OpenStack Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }

  private def handleUnauthorized(filterDirector: FilterDirector, httpServletRequest: HttpServletRequest) {
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
}
