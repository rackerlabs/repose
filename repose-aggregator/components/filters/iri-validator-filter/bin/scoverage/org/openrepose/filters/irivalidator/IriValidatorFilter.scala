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
package org.openrepose.filters.irivalidator

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.StrictLogging
import org.apache.jena.iri.{IRIException, IRIFactory}

/**
 * This filter validates that the request URI is a valid IRI.
 */
class IriValidatorFilter extends Filter with StrictLogging {

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
        httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getViolation.getShortMessage)
    }
  }
}
