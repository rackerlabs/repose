package org.openrepose.filters.loadertestfilterfour.commonfilter

import java.util.logging.{Level, Logger}
import javax.inject.Named
import javax.servlet.http.HttpServletRequest
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse, Filter}

import org.openrepose.filters.loadertestfilterone.commonfilter.SimpleClass

/**
 * Created by dimi5963 on 11/6/14.
 */
@Named
class FourthFilter extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest: HttpServletRequest = request.asInstanceOf[HttpServletRequest]
    Logger.getAnonymousLogger.log(Level.INFO, new SimpleClass().createBar)
    chain.doFilter(httpRequest, response)
  }

  override def destroy(): Unit = {

  }
}
