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
import javax.servlet.http.HttpServletRequest
import javax.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}
import org.openrepose.powerfilter.ReposeFilterChain.FilterContext

class ReposeFilterChain(val filterChain: List[FilterContext], originalChain: FilterChain, bypassUrlRegex: Option[String]) extends FilterChain with StrictLogging {

  override def doFilter(request: ServletRequest, response: ServletResponse): Unit = {
    bypassUrlRegex.map(_.r.pattern.matcher(request.asInstanceOf[HttpServletRequest].getRequestURI).matches()) match {
      case Some(true) =>
        logger.debug("Bypass url hit")
        runNext(List.empty, request, response)
      case _ =>
        runNext(filterChain, request, response)
    }
  }

  def runNext(chain: List[FilterContext], request: ServletRequest, response: ServletResponse): Unit = {
    chain match {
      case Nil =>
        logger.debug("End of the filter chain reached")
        originalChain.doFilter(request, response)
      case head::tail =>
        if (head.shouldRun(request.asInstanceOf[HttpServletRequest])) {
          logger.debug("Entering filter: {}", head.filterName)
          head.filter.doFilter(request, response, new ReposeFilterChain(tail, originalChain, None))
        } else {
          logger.debug("Skipping filter: {}", head.filterName)
          runNext(tail, request, response)
        }
    }
  }
}

object ReposeFilterChain {
  case class FilterContext(filter: Filter, filterName: String, shouldRun: HttpServletRequest => Boolean)
}
