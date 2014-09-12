package com.rackspace.papi.components.openstackidentity.basicauth

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.openstackidentity.basicauth.config.OpenStackIdentityBasicAuthConfig
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.slf4j.LoggerFactory

class OpenStackIdentityBasicAuthHandler(basicAuthConfig: OpenStackIdentityBasicAuthConfig, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[OpenStackIdentityBasicAuthHandler])

  override def handleRequest(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Request")
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // TODO: Handle the Request
    // IF request has a HTTP Basic authentication header (Authorization), THEN ...
    //    IF the userName/apiKey is in the cache,
    //    THEN add the token header;
    //    ELSE
    //     - unbase 64 API userName/apiKey
    //     - request a token
    //       IF a token was received, THEN
    //        - add the token header
    //        - cache the token (configurable cache timeout)
    //       ELSE - NO token received
    //        - set the response status code to 401
    //        - consume the remainder of the filter chain
    // ELSE - NO basic header
    //  - Simply send request goes through
    filterDirector.setFilterAction(FilterAction.PASS)
    filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK) // 200
    filterDirector
  }

  override def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Response. Incoming status code: " + httpServletResponse.getStatus())
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // TODO: Handle the Response
    // IF response Status Code is 401, THEN
    //  - add HTTP Basic authentication header (WWW-Authenticate)
    //    IF request has a HTTP Basic authentication header (Authorization),
    //    THEN remove the userName/apiKey from the cache
    filterDirector.setResponseStatusCode(HttpServletResponse.SC_NO_CONTENT) // 204
    LOG.debug("OpenStack Identity Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }
}
