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
import org.openrepose.filters.cors.CorsFilter.{NonCorsRequest, ActualRequest, PreflightRequest}

class CorsFilter extends Filter with LazyLogging {
  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("CORS filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    val httpServletResponse = servletResponse.asInstanceOf[HttpServletResponse]
    val requestedHeaders = Option(httpServletRequest.getHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString))
    val isOptions = httpServletRequest.getMethod == "OPTIONS"

    val requestType =
      (Option(httpServletRequest.getHeader(CommonHttpHeader.ORIGIN.toString)),
        isOptions,
        Option(httpServletRequest.getHeader(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString))) match {
        case (Some(origin), true, Some(requestedMethod)) => PreflightRequest(origin, requestedMethod)
        case (Some(origin), _, _) => ActualRequest(origin)
        case _ => NonCorsRequest
    }

    requestType match {
      case PreflightRequest(origin, requestedMethod) =>
        httpServletResponse.setHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString, requestedMethod)
        httpServletResponse.setHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString, "true")
        httpServletResponse.setHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString, origin)
        if (requestedHeaders.nonEmpty) {
          httpServletResponse.setHeader(CommonHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString, requestedHeaders.get)
        }
        httpServletResponse.setStatus(HttpServletResponse.SC_OK)
      case NonCorsRequest =>
        filterChain.doFilter(servletRequest, servletResponse)
    }

    httpServletResponse.addHeader(CommonHttpHeader.VARY.toString, CommonHttpHeader.ORIGIN.toString)

    if (isOptions) {
      httpServletResponse.addHeader(CommonHttpHeader.VARY.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString)
      httpServletResponse.addHeader(CommonHttpHeader.VARY.toString, CommonHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString)
    }
  }

  override def destroy(): Unit = {
    logger.trace("CORS filter destroyed")
  }
}

object CorsFilter {
  sealed trait RequestType
  object NonCorsRequest extends RequestType
  case class PreflightRequest(origin: String, requestedMethod: String) extends RequestType
  case class ActualRequest(origin: String) extends RequestType
}