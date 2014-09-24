package com.rackspace.papi.components.rackspace.identity.basicauth

import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.{HttpHeaders, MediaType}

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.rackspace.identity.basicauth.config.RackspaceIdentityBasicAuthConfig
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
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
    val optionXAuthHeaders = Option(httpServletRequest.getHeaders(X_AUTH_TOKEN))
    if (!optionXAuthHeaders.isDefined || !(optionXAuthHeaders.get.hasMoreElements)) {
      val optionHeaders = Option(httpServletRequest.getHeaders(HttpHeaders.AUTHORIZATION))
      val authMethodBasicHeaders = BasicAuthUtils.getBasicAuthHeaders(optionHeaders, "Basic")
      if (authMethodBasicHeaders.isDefined && !(authMethodBasicHeaders.get.isEmpty)) {
        var tokenFound = false
        var code = HttpServletResponse.SC_OK
        val authHeader = authMethodBasicHeaders.get.next()
        val authValue = authHeader.replace("Basic ", "")
        var token = Option(datastore.get(TOKEN_KEY_PREFIX + authValue))
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
          if (token.isDefined && !(token.isEmpty)) {
            val tokenStr = token.get.toString
            if (tokenStr.length > 0) {
              if (tokenCacheTtlMillis > 0) {
                datastore.put(TOKEN_KEY_PREFIX + authValue, token.get.toString, tokenCacheTtlMillis, TimeUnit.MILLISECONDS)
              }
              // add the token header
              filterDirector.requestHeaderManager().appendHeader(X_AUTH_TOKEN, token.get.toString)
              // set the flag
              tokenFound = true
            }
          }
        }
        if (!tokenFound) {
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

    filterDirector
  }

  private def getUserToken(authValue: String): (Int, Option[String]) = {
    val createAuthRequest = (encoded: String) => {
      // Base64 Decode and split the userName/apiKey
      val (userName, apiKey) = BasicAuthUtils.extractCredentials(authValue)
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

    if (authTokenResponse.isDefined) {
      val statusCode = authTokenResponse.get.getStatusCode
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
    if (httpServletResponse.getStatus == HttpServletResponse.SC_UNAUTHORIZED ||
      httpServletResponse.getStatus == HttpServletResponse.SC_FORBIDDEN) {
      handleUnauthorized(filterDirector, httpServletRequest)
    }
    LOG.debug("Rackspace Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }

  private def handleUnauthorized(filterDirector: FilterDirector, httpServletRequest: HttpServletRequest) {
    // add HTTP Basic authentication header (WWW-Authenticate) with the realm of RAX-KEY
    filterDirector.responseHeaderManager().appendHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"RAX-KEY\"")
    val optionHeaders = Option(httpServletRequest.getHeaders(HttpHeaders.AUTHORIZATION))
    val authMethodBasicHeaders = BasicAuthUtils.getBasicAuthHeaders(optionHeaders, "Basic")
    if (authMethodBasicHeaders.isDefined && !(authMethodBasicHeaders.get.isEmpty)) {
      for (authHeader <- authMethodBasicHeaders.get) {
        val authValue = authHeader.replace("Basic ", "")
        datastore.remove(X_AUTH_TOKEN + authValue)
      }
    }
  }
}
