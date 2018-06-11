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
package org.openrepose.powerfilter

import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}

@Named
class ReposeServlet @Inject()(router: PowerFilterRouter) extends HttpServlet with StrictLogging {

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val wrappedRequest = new HttpServletRequestWrapper(req)
    val wrappedResponse = new HttpServletResponseWrapper(resp, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

    try {
      if (wrappedResponse.getStatus() < SC_INTERNAL_SERVER_ERROR) {
        router.route(wrappedRequest, wrappedResponse)
      }
    } catch {
      case e: Exception =>
        logger.error("Failed to route the request to the origin service", e)
        wrappedResponse.uncommit()
        wrappedResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    } finally {
      wrappedResponse.commitToResponse()
    }
  }
}
