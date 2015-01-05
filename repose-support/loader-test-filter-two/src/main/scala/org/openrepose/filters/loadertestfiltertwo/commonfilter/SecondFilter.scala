package org.openrepose.filters.loadertestfiltertwo.commonfilter

import javax.inject.Named
import javax.servlet.http.HttpServletRequest
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse, Filter}

/**
 * Created by dimi5963 on 11/6/14.
 */
@Named
class SecondFilter extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest: HttpServletRequest = request.asInstanceOf[HttpServletRequest]
    httpRequest.getHeader("FOO") match {
      case "BAR" => chain.doFilter(httpRequest, response)
      case _ => throw new IllegalArgumentException("Invalid header")
    }
  }

  override def destroy(): Unit = {

  }
}
