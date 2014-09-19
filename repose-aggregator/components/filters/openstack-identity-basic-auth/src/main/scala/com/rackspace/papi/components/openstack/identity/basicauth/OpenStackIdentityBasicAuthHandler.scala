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
        if (token.isDefined && !(token.isEmpty)) {
          val tokenStr = token.get.toString
          if(tokenStr.length > 0) {
            filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
            tokenFound = true
          }
        } else {
          // request a token
          token = getUserToken(authValue)
          // IF a token was received, THEN ...
          if (token.isDefined && !(token.isEmpty)) {
            val tokenStr = token.get.toString
            if(tokenStr.length > 0) {
              // add the token header
              filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
              // cache the token with the configured cache timeout
              datastore.put(TOKEN_KEY_PREFIX + authValue, token.get.toString, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
              tokenFound = true
            }
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
      // Scala's standard XML syntax does not support the XML declaration w/o a lot of hoops
      //<?xml version="1.0" encoding="UTF-8"?>
      <auth xmlns="http://docs.openstack.org/identity/api/v2.0">
        <apiKeyCredentials
          xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
          username={ userName }
          apiKey={ apiKey }/>
      </auth>
    }
    // Base64 Decode and split the userName/apiKey
    val authTokenResponse = Option(akkaServiceClient.post(authValue,
      identityServiceUri + "/v2.0/tokens",
      Map[String, String]().asJava,
      createJsonAuthRequest(authValue).toString,
      MediaType.APPLICATION_XML_TYPE))

    if (authTokenResponse.isDefined) {
      def authRespDataRaw = authTokenResponse.get.getData
      def authRespDataStr = Source.fromInputStream(authRespDataRaw).mkString
      def xmlString = XML.loadString(authRespDataStr)
      val idString = (xmlString \\ "access" \ "token" \ "@id").text
      Option(idString)
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
