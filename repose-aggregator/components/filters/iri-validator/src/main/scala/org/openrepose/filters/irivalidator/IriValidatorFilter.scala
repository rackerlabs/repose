package org.openrepose.filters.irivalidator

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.jena.iri.{IRIException, IRIFactory}

/**
 * This filter validates that the request URI is a valid IRI.
 */
class IriValidatorFilter extends Filter with LazyLogging {

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("IRI validator filter initialized")
  }

  override def destroy(): Unit = {
    logger.trace("IRI validator filter destroyed")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]
    val requestUrl = httpServletRequest.getRequestURL.toString + httpServletRequest.getQueryString

    // This IRIFactory only verifies the IRI spec define in RFC 3987
    val iriValidator = IRIFactory.iriImplementation()

    try {
      logger.trace("Attempting to validate the request URI as an IRI")
      iriValidator.construct(requestUrl)

      logger.trace("Request URI is a valid IRI, forwarding the request")
      filterChain.doFilter(servletRequest, servletResponse)
    } catch {
      case e: IRIException =>
        logger.error(s"$requestUrl is an invalid IRI, rejecting the request")
        httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST)
    }
  }
}
