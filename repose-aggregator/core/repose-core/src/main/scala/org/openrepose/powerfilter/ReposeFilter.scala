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

import java.net.{URI, URISyntaxException}
import java.util.{Optional, UUID}

import com.codahale.metrics.MetricRegistry
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.StringUtils
import org.openrepose.commons.utils.http.CommonHttpHeader.{REQUEST_ID, TRACE_GUID, VIA}
import org.openrepose.commons.utils.http.CommonRequestAttributes.QUERY_PARAMS
import org.openrepose.commons.utils.http.PowerApiHeader.TRACE_REQUEST
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.io.stream.LimitedReadInputStream
import org.openrepose.commons.utils.logging.apache.format.stock.ResponseTimeHandler.START_TIME_ATTRIBUTE
import org.openrepose.commons.utils.logging.{TracingHeaderHelper, TracingKey}
import org.openrepose.commons.utils.opentracing.ScopeHelper.{closeSpan, startSpan}
import org.openrepose.commons.utils.scala.TryWith
import org.openrepose.commons.utils.servlet.http.ResponseMode.MUTABLE
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.config.{Filter => _}
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.openrepose.powerfilter.ReposeFilter._
import org.slf4j.{LoggerFactory, MDC}
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.filter.DelegatingFilterProxy

import scala.util.{Failure, Success}

@Named("reposeFilter")
class ReposeFilter @Inject()(@Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                             @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVersion: String,
                             optMetricsService: Optional[MetricsService],
                             reposeFilterLoader: ReposeFilterLoader,
                             containerConfigurationService: ContainerConfigurationService,
                             tracer: Tracer,
                             uriRedactionService: UriRedactionService,
                             responseHeaderService: ResponseHeaderService
                            )
  extends DelegatingFilterProxy {

  private val optMetricRegistry: Option[MetricRegistry] = Option(optMetricsService.orElse(null)).map(_.getRegistry)

  override def initFilterBean(): Unit = {
    ThisLogger.info("{} -- Initializing ReposeFilter", nodeId)
    reposeFilterLoader.setServletContext(getServletContext)
    ThisLogger.trace("{} -- initialized.", nodeId)
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    reposeFilterLoader.getFilterContextList match {
      case Some(filterContextList) =>
        ThisLogger.trace("ReposeFilter processing request...")
        val startTime = System.currentTimeMillis
        if (Option(request.asInstanceOf[HttpServletRequest].getHeader(TRACE_REQUEST)).isDefined) {
          MDC.put(TRACE_REQUEST, "true")
        }

        val contentBodyReadLimit = containerConfigurationService.getContentBodyReadLimit
        val requestBodyInputStream =
          if (contentBodyReadLimit.isPresent) {
            new LimitedReadInputStream(contentBodyReadLimit.get, request.getInputStream)
          } else {
            request.getInputStream
          }

        val wrappedResponse = new HttpServletResponseWrapper(response.asInstanceOf[HttpServletResponse], MUTABLE, MUTABLE)
        val bufferedInputStream = new BufferedServletInputStream(requestBodyInputStream)

        // Since getParameterMap may read the body, we must reset the InputStream so that we aren't stripping
        // the body when form parameters are sent.
        bufferedInputStream.mark(Integer.MAX_VALUE)
        val paramaterMap = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest], bufferedInputStream).getParameterMap
        bufferedInputStream.reset()

        // Wrapping the request to reset the inputStream/Reader flag
        val wrappedRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest], bufferedInputStream)

        // Added so HERP has the Query Params available for logging.
        wrappedRequest.setAttribute(QUERY_PARAMS, paramaterMap)

        // Add the start time to be used by the ResponseTimeHandler/HttpLogFormatter.
        wrappedRequest.setAttribute(START_TIME_ATTRIBUTE, startTime)

        val scope = startSpan(wrappedRequest, tracer, ThisLogger, Tags.SPAN_KIND_CLIENT, reposeVersion, uriRedactionService)

        // Conditionally remove the tracing header so it will be overwritten
        val tracingHeaderConfig = reposeFilterLoader.getTracingHeaderConfig
        if (tracingHeaderConfig.exists(_.isRewriteHeader)) wrappedRequest.removeHeader(TRACE_GUID)

        //Grab the traceGUID from the request if there is one, else create one
        val traceGUID = Option(wrappedRequest.getHeader(TRACE_GUID)) match {
          case Some(string) if StringUtils.isNotBlank(string) => string
          case _ => UUID.randomUUID.toString
        }

        MDC.put(TracingKey.TRACING_KEY, traceGUID)

        try {
          TryWith(filterContextList)(filterContextListToo => {
            // Ensure the request URI is a valid URI
            // This object is only being created to ensure its validity.
            // So it is safe to suppress warning squid:S1848
            new URI(wrappedRequest.getRequestURI)

            if (tracingHeaderConfig.exists(_.isEnabled)) {
              if (StringUtils.isBlank(wrappedRequest.getHeader(TRACE_GUID))) {
                wrappedRequest.addHeader(TRACE_GUID, TracingHeaderHelper.createTracingHeader(traceGUID, wrappedRequest.getHeader(VIA)))
              }
              if (tracingHeaderConfig.exists(_.isSecondaryPlainText)) {
                TraceIdLogger.trace("Adding plain text trans id to request: {}", traceGUID)
                wrappedRequest.replaceHeader(REQUEST_ID, traceGUID)
              }
              val tracingHeader = wrappedRequest.getHeader(TRACE_GUID)
              TraceIdLogger.info("Tracing header: {}", TracingHeaderHelper.decode(tracingHeader))
              wrappedResponse.addHeader(TRACE_GUID, tracingHeader)
            }
            new ReposeFilterChain(
              filterContextListToo.filterContexts,
              chain,
              None,
              optMetricRegistry,
              tracer
            ).doFilter(wrappedRequest, wrappedResponse)
          }) match {
            case Success(_) =>
              ThisLogger.trace("{} -- Successfully processed request", nodeId)
            case Failure(use: URISyntaxException) =>
              ThisLogger.debug(s"$nodeId -- Invalid URI requested: ${wrappedRequest.getRequestURI}", use)
              wrappedResponse.sendError(SC_BAD_REQUEST, "Error processing request")
            case Failure(e: Exception) =>
              ThisLogger.error(s"$nodeId -- Issue encountered while processing filter chain.", e)
              wrappedResponse.sendError(SC_BAD_GATEWAY, "Error processing request")
            case Failure(t: Throwable) =>
              ThisLogger.error(s"$nodeId -- Error encountered while processing filter chain.", t)
              wrappedResponse.sendError(SC_BAD_GATEWAY, "Error processing request")
              throw t
          }
        } finally {
          closeSpan(wrappedResponse, scope)
          if (!wrappedResponse.isCommitted) {
            responseHeaderService.setVia(wrappedRequest, wrappedResponse)
          }
          wrappedResponse.commitToResponse()
          optMetricRegistry.foreach { (mr: MetricRegistry) =>
            markResponseCodeHelper(
              mr,
              wrappedResponse.asInstanceOf[HttpServletResponse].getStatus,
              System.currentTimeMillis - startTime)
          }
        }
        // Clear out the logger context now that we are done with this request
        MDC.clear()
        ThisLogger.trace("ReposeFilter returning response...")
      case None =>
        ThisLogger.error("ReposeFilter has not yet initialized...")
        response.asInstanceOf[HttpServletResponse].sendError(SC_INTERNAL_SERVER_ERROR, "ReposeFilter not initialized")
    }
  }

  override def destroy(): Unit = {
    ThisLogger.trace("{} -- destroying ...", nodeId)
    ThisLogger.info("{} -- Destroyed ReposeFilter", nodeId)
  }

}

object ReposeFilter {
  // NOTE: This is to workaround the Spring DelegatingFilterProxy ancestry which provides an Apache Log named logger.
  // NOTE: This provided logger in turn conflicts with Scala's StrictLogging provided logger.
  final val ThisLogger = LoggerFactory.getLogger(classOf[ReposeFilter].getName)
  private final val TraceIdLogger = LoggerFactory.getLogger(s"${classOf[ReposeFilter].getName}.trace-id-logging")

  def markResponseCodeHelper(metricRegistry: MetricRegistry, responseCode: Int, lengthInMillis: Long): Unit = {
    if (100 <= responseCode && responseCode < 600) {
      val statusCodeClass = "%dXX".format(responseCode / 100)
      metricRegistry.meter(
        MetricRegistry.name("org.openrepose.core.ResponseCode.ToClient", "Repose", statusCodeClass)
      ).mark()
      metricRegistry.histogram(
        MetricRegistry.name("org.openrepose.core.ResponseTime.ToClient", "Repose", statusCodeClass)
      ).update(lengthInMillis)
    } else {
      ThisLogger.error(s"Repose: Encountered invalid response code: $responseCode")
    }
  }
}
