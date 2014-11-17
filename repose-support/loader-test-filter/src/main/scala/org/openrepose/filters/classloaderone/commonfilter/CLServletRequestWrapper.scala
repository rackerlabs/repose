package org.openrepose.filters.classloaderone.commonfilter

import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletRequest

import org.apache.commons.lang3.StringUtils


/**
 * Created by dimi5963 on 11/7/14.
 */
class CLServletRequestWrapper(val httpRequest: HttpServletRequest) extends HttpServletRequestWrapper(httpRequest) {
  override def getHeader(p1: String): String = {
    StringUtils.startsWith(p1, "blah")
    super.getHeader(p1)
  }
}
