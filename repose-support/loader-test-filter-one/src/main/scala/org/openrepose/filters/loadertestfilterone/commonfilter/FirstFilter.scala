package org.openrepose.filters.loadertestfilterone.commonfilter

import javax.inject.Named
import javax.servlet.http.HttpServletRequest
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse, Filter}

/**
 * Created by dimi5963 on 11/6/14.
 */
@Named
class FirstFilter extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest: HttpServletRequest = request.asInstanceOf[HttpServletRequest]
    val w: CLServletRequestWrapper = new CLServletRequestWrapper(httpRequest)
    chain.doFilter(w, response)
  }

  override def destroy(): Unit = {

  }
}
