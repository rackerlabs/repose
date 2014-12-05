package org.openrepose.filters.herp

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType

import com.rackspace.httpdelegation._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.generic.SeqFactory
import scala.util.Failure
import scala.util.Success
import scala.util.{Failure, Success}


class HerpFilter extends Filter with HttpDelegationManager {

  private final val LOG = LoggerFactory.getLogger(classOf[HerpFilter])

  override def init(filterConfig: FilterConfig): Unit = {
    LOG.trace("HeRP filter initialized")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {

  }

  override def destroy(): Unit = {
    LOG.trace("HeRP filter destroyed")
  }

}
