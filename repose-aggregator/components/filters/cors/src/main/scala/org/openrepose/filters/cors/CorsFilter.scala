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
package org.openrepose.filters.cors

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.CommonHttpHeader

class CorsFilter extends Filter with LazyLogging {
  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("CORS filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]

    filterChain.doFilter(servletRequest, servletResponse)

    httpServletResponse.addHeader(CommonHttpHeader.VARY.toString, CommonHttpHeader.ORIGIN.toString)

    if (httpServletRequest.getMethod == "OPTIONS") {
      // the response depends on the values of these headers in the request
      httpServletResponse.addHeader(CommonHttpHeader.VARY.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString)
      httpServletResponse.addHeader(CommonHttpHeader.VARY.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString)
    }
  }

  override def destroy(): Unit = {
    logger.trace("CORS filter destroyed")
  }
}