package com.rackspace.papi.components.keystone.v3

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler

class KeystoneV3Handler extends AbstractFilterLogicHandler {

  override def handleRequest(request: HttpServletRequest, response: HttpServletResponse): FilterDirector = {

  }
}
