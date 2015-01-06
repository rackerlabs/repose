package org.openrepose.filters.core.filterfour

import javax.inject.Named
import javax.servlet._
import javax.servlet.http.HttpServletRequest

/**
 * Created by dimi5963 on 1/6/15.
 */
@Named
class FilterFour  extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val r: ServletRequest = new ClassLoaderServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    chain.doFilter(r, response)
  }

  override def destroy(): Unit = {

  }
}

