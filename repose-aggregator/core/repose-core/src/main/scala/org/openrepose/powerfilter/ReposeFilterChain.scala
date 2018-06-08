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
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.annotation.{JsonAutoDetect, PropertyAccessor}
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{Filter, FilterChain, ServletRequest, ServletResponse}
import org.openrepose.commons.utils.io.{BufferedServletInputStream, RawInputStreamReader}
import org.openrepose.commons.utils.servlet.http.ResponseMode.{PASSTHROUGH, READONLY}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.powerfilter.ReposeFilterChain._
import org.openrepose.powerfilter.intrafilterlogging.{RequestLog, ResponseLog}
import org.slf4j.{Logger, LoggerFactory}

class ReposeFilterChain(val filterChain: List[FilterContext], originalChain: FilterChain, bypassUrlRegex: Option[String], metricsRegistry: MetricRegistry)
  extends FilterChain
    with StrictLogging {

  override def doFilter(inboundRequest: ServletRequest, inboundResponse: ServletResponse): Unit = {
    val request = inboundRequest.asInstanceOf[HttpServletRequest]
    val response = inboundResponse.asInstanceOf[HttpServletResponse]
    try {
      bypassUrlRegex.map(_.r.pattern.matcher(request.getRequestURI).matches()) match {
        case Some(true) =>
          logger.debug("Bypass url hit")
          runNext(List.empty, request, response)
        case _ =>
          runNext(filterChain, request, response)
      }
    } catch {
      case e: Exception =>
        logger.error("Exception thrown while processing the chain.", e)
        if (!response.isCommitted) {
          response.sendError(SC_INTERNAL_SERVER_ERROR, "Exception while processing filter chain.")
        }
    }
  }

  def runNext(chain: List[FilterContext], request: HttpServletRequest, response: HttpServletResponse): Unit = {
    chain match {
      case Nil =>
        doIntrafilterLogging(request, response, "origin", (intraRequest, intraResponse) => {
          doMetrics(intraRequest, intraResponse, "origin", (metricsRequest, metricsResponse) => {
            logger.debug("End of the filter chain reached")
            originalChain.doFilter(metricsRequest, metricsResponse)
          })
        })
      case head::tail =>
        if (head.shouldRun(request)) {
          doIntrafilterLogging(request, response, head.filterName, (intraRequest, intraResponse) => {
            doMetrics(intraRequest, intraResponse, head.filterName, (metricsRequest, metricsResponse) => {
              logger.debug("Entering filter: {}", head.filterName)
              head.filter.doFilter(metricsRequest, metricsResponse, new ReposeFilterChain(tail, originalChain, None, metricsRegistry))
            })
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

  def doMetrics(request: HttpServletRequest, response: HttpServletResponse, filter: String, requestProcess: (HttpServletRequest, HttpServletResponse) => Unit): Unit = {

    val startTime = System.currentTimeMillis()

    requestProcess(request, response)

    val elapsedTime = System.currentTimeMillis() - startTime

    metricsRegistry.timer(MetricRegistry.name(FilterProcessingMetric, filter))
                   .update(elapsedTime, TimeUnit.MILLISECONDS)

    if (Option(request.getHeader(TracingHeader)).isDefined && !response.isCommitted) {
      response.addHeader(s"X-$filter-Time", s"${elapsedTime}ms")
    }
  }
}

object ReposeFilterChain {
  case class FilterContext(filter: Filter, filterName: String, shouldRun: HttpServletRequest => Boolean)

  val IntrafilterLog: Logger = LoggerFactory.getLogger("intrafilter-logging")

  val IntrafilterObjectMapper: ObjectMapper = new ObjectMapper
  IntrafilterObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY) //http://stackoverflow.com/a/8395924

  val FilterProcessingMetric: String = "org.openrepose.core.FilterProcessingTime.Delay"

  val TracingHeader: String = "X-Trace-Request"
}
