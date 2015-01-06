package org.openrepose.filters.core.filterthree

import javax.inject.Named
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.openrepose.others.SimplicityDivine

/**
 * Created by dimi5963 on 1/6/15.
 */
@Named
class FilterThree extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    new SimplicityDivine().createBar
    chain.doFilter(request, response)
  }

  override def destroy(): Unit = {

  }
}

