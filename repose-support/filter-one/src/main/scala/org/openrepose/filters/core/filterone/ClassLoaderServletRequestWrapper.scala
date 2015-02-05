package org.openrepose.filters.core.filterone

import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

import org.apache.commons.lang3.StringUtils

import org.openrepose.others.SimplicityDivine

class ClassLoaderServletRequestWrapper(request: HttpServletRequest) extends HttpServletRequestWrapper(request) {
  override def getHeader(headerString: String): String = {
    println("\n\n\n")
    println(s"Requesting header ${headerString}")
    println("\n\n\n")
    if(StringUtils.startsWith(headerString, "FOO")){
      return new SimplicityDivine().createBar
    } else {
      return super.getHeader(headerString)
    }
  }
}
