package org.openrepose.filters.classloaderone.commonfilter

import javax.servlet.http.HttpServletRequestWrapper

import org.apache.commons.lang3.StringUtils


/**
 * Created by dimi5963 on 11/7/14.
 */
class CLServletRequestWrapper extends HttpServletRequestWrapper {
  override def getHeader(p1: String): String = {
    StringUtils.startsWith(p1, "blah")
    super.getHeader()
  }
}
