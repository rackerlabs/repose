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
package org.openrepose.filters.forwardedproto

import javax.servlet._
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.StrictLogging
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper

/**
 * The sole purpose of this filter is to add the X-Forwarded-Proto header to a request with a value which
 * corresponds to the protocol of the request (e.g. http or https).
 */
class ForwardedProtoFilter extends Filter with StrictLogging {

  private final val X_FORWARDED_PROTO = "X-Forwarded-Proto"

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("ForwardedProto filter initialized")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]

    if (Option(httpServletRequest.getHeader(X_FORWARDED_PROTO)).isEmpty) {
      val headerValue = servletRequest.getProtocol.substring(0, servletRequest.getProtocol.indexOf('/'))
      logger.debug(s"Adding the $X_FORWARDED_PROTO header with value $headerValue")

      val wrappedHttpServletRequest = new HttpServletRequestWrapper(httpServletRequest)
      wrappedHttpServletRequest.addHeader(X_FORWARDED_PROTO, headerValue)

      filterChain.doFilter(wrappedHttpServletRequest, servletResponse)
    } else {
      logger.debug("Passing the request without modifying headers")
      filterChain.doFilter(servletRequest, servletResponse)
    }
  }

  override def destroy(): Unit = {
    logger.trace("ForwardedProto filter destroyed")
  }
}
