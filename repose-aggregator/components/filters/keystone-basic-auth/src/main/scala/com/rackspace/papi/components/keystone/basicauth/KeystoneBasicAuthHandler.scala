package com.rackspace.papi.components.keystone.basicauth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.keystone.basicauth.config.KeystoneBasicAuthConfig
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.rackspace.papi.filter.logic.{FilterAction, FilterDirector}
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.slf4j.LoggerFactory

class KeystoneBasicAuthHandler(keystoneConfig: KeystoneBasicAuthConfig, akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractFilterLogicHandler {

  private final val LOG = LoggerFactory.getLogger(classOf[KeystoneBasicAuthHandler])

  override def handleRequest(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    LOG.debug("Handling HTTP Request")
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // TODO: Handle the Request
    filterDirector.setFilterAction(FilterAction.PASS)
    filterDirector
  }

  override def handleResponse(httpServletRequest: HttpServletRequest, httpServletResponse: ReadableHttpServletResponse): FilterDirector = {
    // TODO: This should work, but due to what appears to be a limitation of the current ScalaMock it causes an error during testing.
    //LOG.debug("Handling HTTP Response. Incoming status code: " + httpServletResponse.getStatus())
    val filterDirector: FilterDirector = new FilterDirectorImpl()
    // TODO: Handle the Response
    filterDirector.setResponseStatusCode(HttpServletResponse.SC_NO_CONTENT)
    LOG.debug("Keystone Basic Auth Response. Outgoing status code: " + filterDirector.getResponseStatus.intValue)
    filterDirector
  }
}
