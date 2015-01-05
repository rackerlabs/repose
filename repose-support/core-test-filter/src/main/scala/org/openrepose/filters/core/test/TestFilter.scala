package org.openrepose.filters.core.test

import javax.inject.Named
import javax.servlet._
import javax.servlet.http.HttpServletRequest

/**
 * This test filter assumes it is operating in the test classpath of core unit tests
 */
@Named
class TestFilter extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    request.asInstanceOf[HttpServletRequest].getHeader("something")
    chain.doFilter(request,response)
  }

  override def destroy(): Unit = {

  }
}
