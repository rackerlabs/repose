package org.openrepose.filters.forwardedproto

import javax.servlet._
import javax.servlet.http.HttpServletRequest

import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest

/**
 * The sole purpose of this filter is to add the X-Forwarded-Proto header to a request with a value which
 * corresponds to the protocol of the request (e.g., http or https).
 */
class ForwardedProtoFilter extends Filter with LazyLogging {

  private final val X_FORWARDED_PROTO = "X-Forwarded-Proto"

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("ForwardedProto filter initialized")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]

    if (Option(httpServletRequest.getHeader(X_FORWARDED_PROTO)).isEmpty) {
      logger.debug(s"Adding the $X_FORWARDED_PROTO header")

      val mutableHttpServletRequest = MutableHttpServletRequest.wrap(httpServletRequest)
      mutableHttpServletRequest.addHeader(X_FORWARDED_PROTO, servletRequest.getProtocol.substring(0, servletRequest.getProtocol.indexOf('/')))

      filterChain.doFilter(mutableHttpServletRequest, servletResponse)
    } else {
      logger.debug("Passing the request without modifying headers")
      filterChain.doFilter(servletRequest, servletResponse)
    }
  }

  override def destroy(): Unit = {
    logger.trace("ForwardedProto filter destroyed")
  }
}
