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
import com.typesafe.scalalogging.StrictLogging
import io.opentracing.Tracer
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletRequest, ServletResponse}
import org.openrepose.commons.utils.http.PowerApiHeader.TRACE_REQUEST
import org.openrepose.commons.utils.io.{BufferedServletInputStream, RawInputStreamReader}
import org.openrepose.commons.utils.logging.HttpLoggingContextHelper
import org.openrepose.commons.utils.servlet.http.ResponseMode.{PASSTHROUGH, READONLY}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.powerfilter.ReposeFilterChain._
import org.openrepose.powerfilter.ReposeFilterLoader.FilterContext
import org.openrepose.powerfilter.intrafilterlogging.{RequestLog, ResponseLog}
import org.slf4j.{Logger, LoggerFactory, MDC}

class ReposeFilterChain(val filterChain: List[FilterContext],
                        originalChain: FilterChain,
                        optBypassUriRegex: Option[String],
                        optMetricRegistry: Option[MetricRegistry],
                        tracer: Tracer)
  extends FilterChain
    with StrictLogging {

  override def doFilter(inboundRequest: ServletRequest, inboundResponse: ServletResponse): Unit = {
    val request = new HttpServletRequestWrapper(inboundRequest.asInstanceOf[HttpServletRequest])
    val response = inboundResponse.asInstanceOf[HttpServletResponse]
    try {
      if (optBypassUriRegex.exists(_.r.pattern.matcher(request.getRequestURI).matches())) {
        logger.debug("Bypass url hit")
        runNext(List.empty, request, response)
      } else {
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

  def runNext(chain: List[FilterContext], request: HttpServletRequestWrapper, response: HttpServletResponse): Unit = {
    chain match {
      case Nil =>
        logger.debug("End of the filter chain reached")
        (doIntrafilterLogging("origin")(_))
          .compose(doMetrics("origin"))
          .compose(updateLoggingContext)
          .apply(originalChain.doFilter(_, _))
          .apply(request, response)
      case head :: tail if head.shouldRun(request) =>
        val filterName = head.filterConfig.getName
        logger.debug("Entering filter: {}", filterName)
        (doIntrafilterLogging(filterName)(_))
          .compose(doMetrics(filterName))
          .compose(updateLoggingContext)
          .apply(head.filter.doFilter(_, _, new ReposeFilterChain(tail, originalChain, None, optMetricRegistry, tracer)))
          .apply(request, response)
      case head :: tail =>
        logger.debug("Skipping filter: {}", head.filterConfig.getName)
        runNext(tail, request, response)
    }
  }

  def doIntrafilterLogging(filter: String)(requestProcess: (HttpServletRequest, HttpServletResponse) => Unit): (HttpServletRequest, HttpServletResponse) => Unit = (request, response) => {
    var conditionallyWrappedRequest = request
    var conditionallyWrappedResponse = response

    val doLogging = IntrafilterLog.isTraceEnabled && Option(MDC.get(TRACE_REQUEST)).isDefined

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

  def doMetrics(filter: String)(requestProcess: (HttpServletRequest, HttpServletResponse) => Unit): (HttpServletRequest, HttpServletResponse) => Unit = (request, response) => {
    val startTime = System.currentTimeMillis()

    val scope = tracer.buildSpan(s"Filter $filter").startActive(true)

    requestProcess(request, response)

    scope.close()

    val elapsedTime = System.currentTimeMillis() - startTime
    optMetricRegistry.foreach(_.timer(MetricRegistry.name(FilterProcessingMetric, filter)).update(elapsedTime, TimeUnit.MILLISECONDS))

    FilterTimingLog.trace("Filter {} spent {}ms processing", filter, elapsedTime)
  }

  def updateLoggingContext(requestProcess: (HttpServletRequest, HttpServletResponse) => Unit): (HttpServletRequest, HttpServletResponse) => Unit = (request, response) => {
    Option(HttpLoggingContextHelper.extractFromRequest(request)).foreach { loggingContext =>
      loggingContext.setOutboundRequest(request)
      logger.trace("Updated the outbound request to {} on the HTTP Logging Service context {}", request, s"${loggingContext.hashCode()}")
    }

    requestProcess(request, response)
  }
}

object ReposeFilterChain {
  final val FilterTimingLog: Logger = LoggerFactory.getLogger("filter-timing")
  final val IntrafilterLog: Logger = LoggerFactory.getLogger("intrafilter-logging")

  final val IntrafilterObjectMapper: ObjectMapper = new ObjectMapper
  IntrafilterObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY) //http://stackoverflow.com/a/8395924

  final val FilterProcessingMetric: String = "org.openrepose.core.FilterProcessingTime.Delay"
}
