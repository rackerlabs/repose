package com.rackspace.identity.repose.authIdentity

import javax.servlet.http.HttpServletRequest

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl

class RackspaceAuthIdentityHandler(config: RackspaceAuthIdentityConfig) extends AbstractFilterLogicHandler {
  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val director = new FilterDirectorImpl()
    Option(config.getV11).map { v11 =>
      val group = v11.getGroup
      val quality = v11.getQuality
      request.getContentType

      //IF content-type is json, get the payload
      //If content-type is xml, xpath out the data
      //this logic will probably be exactly the same for the v20 stuff... (maybe)
      if(request.getContentType.matches("xml")) {
        //It's probably xml, lets try to xpath it
      } else {
        //Try to run it through the JSON pather
      }

    }
    Option(config.getV20).map{ v20 =>
      val group = v20.getGroup
      val quality = v20.getQuality

    }

    director
  }

  //Response will always pass through in this one
}
