package org.openrepose.filters.core.brokenfilter

import javax.inject.Named
import javax.servlet._

@Named
class BrokenFilter extends Filter{
  override def init(p1: FilterConfig): Unit = {
    throw new ServletException("WELL MY HEAD ASPLODE")
  }

  override def doFilter(p1: ServletRequest, p2: ServletResponse, p3: FilterChain): Unit = {}

  override def destroy(): Unit = {

  }
}
