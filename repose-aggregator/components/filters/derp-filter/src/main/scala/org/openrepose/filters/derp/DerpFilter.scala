/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.derp

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType

import com.rackspace.httpdelegation._
import com.typesafe.scalalogging.slf4j.StrictLogging

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

/**
  * The sole purpose of this filter is to reject any request with a header indicating that the request has been
  * delegated.
  *
  * This filter is header quality aware; the delegation header with the highest quality will be used to formulate a
  * response.
  *
  * @deprecated in favor of the HTTP Logging Service
  */
@Deprecated
class DerpFilter extends Filter with HttpDelegationManager with StrictLogging {

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("DeRP filter initialized")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val delegationValues = httpServletRequest.getHeaders(HttpDelegationHeaderNames.Delegated).asScala.toSeq

    if (delegationValues.isEmpty) {
      logger.debug("No delegation header present, forwarding the request")
      filterChain.doFilter(servletRequest, servletResponse)
    } else {
      val sortedErrors = parseDelegationValues(delegationValues).sortWith(_.quality > _.quality)
      val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]

      sortedErrors match {
        case Seq() =>
          logger.warn("No delegation header could be parsed, returning a 500 response")
          httpServletResponse.sendError(500, "Delegation header found but could not be parsed")
        case Seq(preferredValue, _*) =>
          logger.debug(s"Delegation header(s) present, returning a ${preferredValue.statusCode} response")
          httpServletResponse.sendError(preferredValue.statusCode, preferredValue.message)
      }
    }
  }

  def parseDelegationValues(delegationValues: Seq[String]): Seq[HttpDelegationHeader] = {
    delegationValues.flatMap { value =>
      parseDelegationHeader(value) match {
        case Success(delegationHeader) => Some(delegationHeader)
        case Failure(e) =>
          logger.warn("Failed to parse a delegation header: " + e.getMessage)
          None
      }
    }
  }

  override def destroy(): Unit = {
    logger.trace("DeRP filter destroyed")
  }
}
