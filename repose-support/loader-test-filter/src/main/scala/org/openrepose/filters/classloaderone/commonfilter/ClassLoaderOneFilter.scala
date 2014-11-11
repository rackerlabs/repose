package org.openrepose.filters.classloaderone.commonfilter

import javax.inject.Named
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse}

/**
 * Created by dimi5963 on 11/6/14.
 */
@Named
class ClassLoaderOneFilter extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    CLServletRequestWrapper w = new CLServletRequestWrapper(request)
    chain.doFilter(w, response)
  }

  override def destroy(): Unit = {

  }
}
