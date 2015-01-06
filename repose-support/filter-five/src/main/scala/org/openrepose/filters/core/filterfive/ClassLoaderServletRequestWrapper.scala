package org.openrepose.filters.core.filterfive

import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

import org.apache.commons.lang3.StringUtils

import org.openrepose.others.SimplicityDivine

/**
 * Created by dimi5963 on 1/6/15.
 */
class ClassLoaderServletRequestWrapper(request: HttpServletRequest) extends HttpServletRequestWrapper(request) {
  override def getHeader(headerString: String): String = {
    if(StringUtils.startsWith(headerString, "FOO")){
      return new SimplicityDivine().createBar
    } else {
      return super.getHeader(headerString)
    }
  }
}
