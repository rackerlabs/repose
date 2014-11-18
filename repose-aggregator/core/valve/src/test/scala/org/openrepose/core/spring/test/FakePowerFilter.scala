package org.openrepose.core.spring.test

import javax.inject.Named
import javax.servlet._

@Named("powerFilter")
class FakePowerFilter extends Filter {
  override def init(filterConfig: FilterConfig): Unit = {

  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    filterChain.doFilter(servletRequest, servletResponse)
  }

  override def destroy(): Unit = {

  }
}
