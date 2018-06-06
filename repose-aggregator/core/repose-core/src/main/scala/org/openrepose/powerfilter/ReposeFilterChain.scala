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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.fasterxml.jackson.annotation.{JsonAutoDetect, PropertyAccessor}
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}
import org.openrepose.commons.utils.io.{BufferedServletInputStream, RawInputStreamReader}
import org.openrepose.commons.utils.servlet.http.ResponseMode.{PASSTHROUGH, READONLY}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.powerfilter.ReposeFilterChain.{FilterContext, IntrafilterLog, IntrafilterObjectMapper}
import org.openrepose.powerfilter.intrafilterlogging.{RequestLog, ResponseLog}
import org.slf4j.{Logger, LoggerFactory}

class ReposeFilterChain(val filterChain: List[FilterContext], originalChain: FilterChain, bypassUrlRegex: Option[String]) extends FilterChain with StrictLogging {

  override def doFilter(inboundRequest: ServletRequest, inboundResponse: ServletResponse): Unit = {
    val request = inboundRequest.asInstanceOf[HttpServletRequest]
    val response = inboundResponse.asInstanceOf[HttpServletResponse]
    bypassUrlRegex.map(_.r.pattern.matcher(request.getRequestURI).matches()) match {
      case Some(true) =>
        logger.debug("Bypass url hit")
        runNext(List.empty, request, response)
      case _ =>
        runNext(filterChain, request, response)
    }
  }

  def runNext(chain: List[FilterContext], request: HttpServletRequest, response: HttpServletResponse): Unit = {
    chain match {
      case Nil =>
        doIntrafilterLogging(request, response, "origin", (intraRequest, intraResponse) => {
          logger.debug("End of the filter chain reached")
          originalChain.doFilter(intraRequest, intraResponse)
        })
      case head::tail =>
        if (head.shouldRun(request)) {
          doIntrafilterLogging(request, response, head.filterName, (intraRequest, intraResponse) => {
            logger.debug("Entering filter: {}", head.filterName)
            head.filter.doFilter(intraRequest, intraResponse, new ReposeFilterChain(tail, originalChain, None))
          })
        } else {
          logger.debug("Skipping filter: {}", head.filterName)
          runNext(tail, request, response)
        }
    }
  }

  def doIntrafilterLogging(request: HttpServletRequest, response: HttpServletResponse, filter: String, requestProcess: (HttpServletRequest, HttpServletResponse) => Unit): Unit = {
    var conditionallyWrappedRequest = request
    var conditionallyWrappedResponse = response

    val doLogging = IntrafilterLog.isTraceEnabled

    if (doLogging) {
      var inputStream = request.getInputStream

      //if mark isn't supported we have to wrap the stream up in something that does
      if (!inputStream.markSupported()) {
        val sourceEntity = new ByteArrayOutputStream()
        RawInputStreamReader.instance.copyTo(inputStream, sourceEntity)
        inputStream = new BufferedServletInputStream(new ByteArrayInputStream(sourceEntity.toByteArray))
      }

      conditionallyWrappedRequest = new HttpServletRequestWrapper(conditionallyWrappedRequest, inputStream)
      conditionallyWrappedResponse = new HttpServletResponseWrapper(conditionallyWrappedResponse, PASSTHROUGH, READONLY)

      IntrafilterLog.trace(IntrafilterObjectMapper.writeValueAsString(new RequestLog(conditionallyWrappedRequest.asInstanceOf[HttpServletRequestWrapper], filter)))
    }

    requestProcess(conditionallyWrappedRequest, conditionallyWrappedResponse)

    if (doLogging) {
      IntrafilterLog.trace(IntrafilterObjectMapper.writeValueAsString(new ResponseLog(conditionallyWrappedResponse.asInstanceOf[HttpServletResponseWrapper], filter)))
    }
  }
}

object ReposeFilterChain {
  case class FilterContext(filter: Filter, filterName: String, shouldRun: HttpServletRequest => Boolean)

  val IntrafilterLog: Logger = LoggerFactory.getLogger("intrafilter-logging")

  val IntrafilterObjectMapper: ObjectMapper = new ObjectMapper
  IntrafilterObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY) //http://stackoverflow.com/a/8395924

}
