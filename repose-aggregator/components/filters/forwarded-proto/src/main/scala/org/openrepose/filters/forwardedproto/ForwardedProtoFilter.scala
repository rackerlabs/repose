package org.openrepose.filters.forwardedproto

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType

import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.{CommonHttpHeader, HttpStatusCode, OpenStackServiceHeader}
import org.openrepose.core.filter.logic.FilterAction
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.services.context.ServletContextHelper
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}


class ForwardedProtoFilter extends Filter with HttpDelegationManager with LazyLogging {

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("ForwardedProtoFilter filter initialized")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    logger.trace("X-Forwarded-Proto filter handling request...")

    //stuff?
    handleRequest(servletRequest.asInstanceOf[HttpServletRequest], servletResponse.asInstanceOf[HttpServletResponse])
  }

  override def destroy(): Unit = {
    logger.trace("X-Forwarded-Proto filter destroyed")
  }

  private def handleRequest(httpServletRequest: HttpServletRequest,
                             httpServletResponse: HttpServletResponse) = {
    val filterDirector = new FilterDirectorImpl()
    val headerManager = filterDirector.requestHeaderManager()

    headerManager.putHeader("X-Forwarded-Proto", httpServletRequest.getProtocol())

    filterDirector
  }
}
