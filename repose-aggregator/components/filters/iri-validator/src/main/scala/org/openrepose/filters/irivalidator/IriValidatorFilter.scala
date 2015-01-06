package org.openrepose.filters.irivalidator

import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * This filter validates that the request URI is a valid IRI.
 */
class IriValidatorFilter extends Filter with LazyLogging {

  override def init(filterConfig: FilterConfig): Unit = ???

  override def destroy(): Unit = ???

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = ???
}
