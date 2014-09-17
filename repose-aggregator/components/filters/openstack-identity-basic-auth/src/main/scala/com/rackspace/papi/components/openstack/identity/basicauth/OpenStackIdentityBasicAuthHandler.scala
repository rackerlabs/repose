package com.rackspace.papi.components.openstack.identity.basicauth

import java.io.InputStream
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

class OpenStackIdentityBasicAuthHandler(basicAuthConfig: OpenStackIdentityBasicAuthConfig, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[OpenStackIdentityBasicAuthHandler])
  //private val identityServiceUri = basicAuthConfig.getOpenstackIdentityService.getUri
  private val tokenCacheTtl = basicAuthConfig.getTokenCacheTimeout
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
        val token = Option(datastore.get(authValue))
        // IF the userName/apiKey is in the cache,
        // THEN add the token header;
        // ELSE ...
        if (token.isDefined) {
          filterDirector.requestHeaderManager().appendHeader("TODO_HEADER_NAME", token.get.toString)
          tokenFound = true
        } else {
          // Base64 Decode and split the userName/apiKey
          val (userName, apiKey) = BasicAuthUtils.extractCreds(authValue)
          // request a token
          val token2 = getUserTokenFromUserNameAPIKey(userName, apiKey)
          // IF a token was received, THEN ...
          if (token2._2.isDefined) {
            // add the token header
            filterDirector.requestHeaderManager().appendHeader("TODO_HEADER_NAME", token2._2.get)
            // cache the token with the configured cache timeout
            datastore.put(authValue, token, tokenCacheTtl, TimeUnit.MILLISECONDS)
            tokenFound = true
          }
        }
      }
      if (!tokenFound) {
        // set the response status code to UNAUTHORIZED (401)
        filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED)
        //// set the response status code to FORBIDDEN (403)
        //filterDirector.setResponseStatusCode(HttpServletResponse.SC_FORBIDDEN)
      }
    }
    // No matter what, we need to process the response.
    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE)
    filterDirector
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
          datastore.remove(authValue)
        }
      }
    }
    LOG.debug("OpenStack Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }

  private def getAdminToken(): Option[String] = {
//    val adminName = "TODO: GET_ADMIN_NAME"
//    val adminPass = "TODO: GET_ADMIN_PASS"
//    val payload = s"""
//      |{
//      |  "auth": {
//      |    "passwordCredentials": {
//      |      "username": "${adminName}",
//      |      "password": "${adminPass}"
//      |    }
//      |  }
//      |}
//      """.stripMargin
    None
  }

  private def getTokenFromAuthResponse(data: InputStream): Option[String] = {
    None
  }

  private def getUserTokenFromUserNameAPIKey(userName: String, apiKey: String): (Int,Option[String]) = {
    val adminToken = getAdminToken()
    if(adminToken.isDefined) {
      val payload = s"""
      |{
      |  "auth": {
      |    "RAX-KSKEY:apiKeyCredentials": {
      |      "username": "${userName}",
      |      "apiKey": "${apiKey}"
      |    }
      |  }
      |}
      """.stripMargin

      val serviceClientResponse = akkaServiceClient.post(
        adminToken.get,
        /*identityServiceUri +*/ "/v2.0/tokens",
        new java.util.HashMap[String,String](),
        payload,
        MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE)
      serviceClientResponse.getStatusCode match {
        case HttpServletResponse.SC_OK =>
          (serviceClientResponse.getStatusCode,
          getTokenFromAuthResponse(serviceClientResponse.getData))
        case _ => (serviceClientResponse.getStatusCode, None)
      }
    } else {
      (HttpServletResponse.SC_SERVICE_UNAVAILABLE, None)
    }
  }
}
