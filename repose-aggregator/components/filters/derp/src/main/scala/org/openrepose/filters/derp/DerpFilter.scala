package org.openrepose.filters.derp

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.{HttpDelegationHeaderBean, HttpDelegationHeaders, HttpDelegationManager}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

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
      val sortedErrors = parseDelegationValues(delegationValues).sortWith(_.quality > _.quality)

      sortedErrors match {
        case Seq() =>
          LOG.warn("No delegation header could be parsed so the request will be forwarded")
          filterChain.doFilter(servletRequest, servletResponse)
        case Seq(preferredValue, _*) =>
          servletResponse.asInstanceOf[HttpServletResponse].sendError(preferredValue.statusCode, preferredValue.message)
      }
    }
  }

  override def destroy(): Unit = {}

  def parseDelegationValues(delegationValues: Seq[String]): Seq[HttpDelegationHeaderBean] = {
    // TODO: Performance concerns
    delegationValues.map(parseDelegationHeader).filter {
      case Success(_) =>
        true
      case Failure(e) =>
        LOG.warn("Failed to parse a delegation header: " + e.getMessage)
        false
    }.map(_.get)
  }
}
