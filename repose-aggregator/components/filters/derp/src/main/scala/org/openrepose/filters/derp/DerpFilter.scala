package org.openrepose.filters.derp

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.{HttpDelegationHeaders, HttpDelegationManager}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * The sole purpose of this filter is to reject any request with a header indicating that the request has been
 * delegated.
 *
 * This filter is header quality aware; the delegation header with the highest quality will be used to formulate a
 * response.
 */
class DerpFilter extends Filter with HttpDelegationManager {

  private final val LOG = LoggerFactory.getLogger(classOf[DerpFilter])

  override def init(filterConfig: FilterConfig): Unit = {}

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val delegationValues = httpServletRequest.getHeaders(HttpDelegationHeaders.Delegated).asScala.toSeq

    if (delegationValues.isEmpty) {
      filterChain.doFilter(servletRequest, servletResponse)
    } else {
      // TODO: Log and handle case where parsing fails (parseDelegationHeader(...) function could return a Failure)
      val sortedErrors = delegationValues.map(parseDelegationHeader(_).get).sortWith(_.quality > _.quality)
      val preferredError = sortedErrors.head

      servletResponse.asInstanceOf[HttpServletResponse].sendError(preferredError.statusCode, preferredError.message)
    }
  }

  override def destroy(): Unit = {}
}
