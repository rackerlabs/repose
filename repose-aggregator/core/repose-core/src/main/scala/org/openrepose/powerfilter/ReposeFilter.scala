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
import java.util.concurrent.TimeUnit
import java.util.{Optional, UUID}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.methods._
import org.openrepose.commons.utils.http.CommonHttpHeader.{REQUEST_ID, TRACE_GUID, VIA}
import org.openrepose.commons.utils.http.CommonRequestAttributes.{QUERY_PARAMS, REQUEST_URL}
import org.openrepose.commons.utils.http.PowerApiHeader.TRACE_REQUEST
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.io.stream.LimitedReadInputStream
import org.openrepose.commons.utils.logging.apache.format.stock.ResponseTimeHandler.START_TIME_ATTRIBUTE
import org.openrepose.commons.utils.logging.{TracingHeaderHelper, TracingKey}
import org.openrepose.commons.utils.opentracing.ScopeHelper.{closeSpan, startSpan}
import org.openrepose.commons.utils.servlet.http.ResponseMode.MUTABLE
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.config.{Filter => _}
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.openrepose.powerfilter.ReposeFilter._
import org.slf4j.{Logger, LoggerFactory, MDC}
import org.springframework.beans.factory.annotation.Value

@Named("reposeFilter")
class ReposeFilter @Inject()(@Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                             @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVersion: String,
                             optMetricsService: Optional[MetricsService],
                             reposeFilterLoader: ReposeFilterLoader,
                             containerConfigurationService: ContainerConfigurationService,
                             healthCheckService: HealthCheckService,
                             tracer: Tracer,
                             uriRedactionService: UriRedactionService,
                             responseHeaderService: ResponseHeaderService
                            )
  extends Filter with StrictLogging {

  private val optMetricRegistry: Option[MetricRegistry] = Option(optMetricsService.orElse(null)).map(_.getRegistry)

  override def init(filterConfig: FilterConfig): Unit = {
    logger.info("{} -- Initializing ReposeFilter", nodeId)
    reposeFilterLoader.setServletContext(filterConfig.getServletContext)
    logger.trace("{} -- initialized.", nodeId)
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    if (!healthCheckService.isHealthy) {
      logger.warn("Request cannot be serviced -- Repose is unhealthy")
      response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Currently unable to serve requests")
    } else {
      processRequest(request.asInstanceOf[HttpServletRequest], response.asInstanceOf[HttpServletResponse], chain)
    }
  }

  override def destroy(): Unit = {
    logger.trace("{} -- destroying ...", nodeId)
    logger.info("{} -- Destroyed ReposeFilter", nodeId)
  }

  private def processRequest(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain): Unit = {
    reposeFilterLoader.getFilterContextList match {
      case Some(filterContextList) =>
        logger.trace("ReposeFilter processing request...")
        val startTime = System.currentTimeMillis
        if (Option(request.getHeader(TRACE_REQUEST)).isDefined) {
          MDC.put(TRACE_REQUEST, "true")
        }

        val requestBodyInputStream =
          Option(containerConfigurationService
            .getContentBodyReadLimit
            .orElse(null)) match {
            case Some(readLimit) => new LimitedReadInputStream(readLimit, request.getInputStream)
            case _ => request.getInputStream
          }

        val wrappedResponse = new HttpServletResponseWrapper(response, MUTABLE, MUTABLE)
        val bufferedInputStream = new BufferedServletInputStream(requestBodyInputStream)

        // Since getParameterMap may read the body, we must reset the InputStream so that we aren't stripping
        // the body when form parameters are sent.
        bufferedInputStream.mark(Integer.MAX_VALUE)
        val paramaterMap = new HttpServletRequestWrapper(request, bufferedInputStream).getParameterMap
        bufferedInputStream.reset()

        // Wrapping the request to reset the inputStream/Reader flag
        val wrappedRequest = new HttpServletRequestWrapper(request, bufferedInputStream)

        // Added so HERP has the Original Request URL and the Query Params available for logging.
        wrappedRequest.setAttribute(REQUEST_URL, wrappedRequest.getRequestURL().toString)
        wrappedRequest.setAttribute(QUERY_PARAMS, paramaterMap)

        // Add the start time to be used by the ResponseTimeHandler/HttpLogFormatter.
        wrappedRequest.setAttribute(START_TIME_ATTRIBUTE, startTime)

        val scope = startSpan(wrappedRequest, tracer, logger.underlying, Tags.SPAN_KIND_CLIENT, reposeVersion, uriRedactionService)

        // Conditionally remove the tracing header so it will be overwritten
        val tracingHeaderConfig = reposeFilterLoader.getTracingHeaderConfig
        if (tracingHeaderConfig.exists(_.isRewriteHeader)) wrappedRequest.removeHeader(TRACE_GUID)

        //Grab the traceGUID from the request if there is one, else create one
        val traceGUID = Option(wrappedRequest.getHeader(TRACE_GUID)) match {
          case Some(string) if StringUtils.isNotBlank(string) => TracingHeaderHelper.getTraceGuid(string)
          case _ => UUID.randomUUID.toString
        }

        MDC.put(TracingKey.TRACING_KEY, traceGUID)

        try {
          // Ensure that the method name is supported
          // todo: HTTP request methods are case-sensitive, so this check should not upper case the request method
          if (!SupportedHttpMethods.contains(request.getMethod.toUpperCase)) {
            throw new InvalidMethodException(wrappedRequest.getMethod + " method not supported")
          }

          // Ensure the request URI is a valid URI
          // This object is only being created to ensure its validity.
          // So it is safe to suppress warning squid:S1848
          new URI(wrappedRequest.getRequestURI)

          if (tracingHeaderConfig.forall(_.isEnabled)) {
            if (StringUtils.isBlank(wrappedRequest.getHeader(TRACE_GUID))) {
              wrappedRequest.replaceHeader(TRACE_GUID, TracingHeaderHelper.createTracingHeader(traceGUID, wrappedRequest.getHeader(VIA)))
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
            filterContextList.filterContexts,
            chain,
            filterContextList.bypassUriRegex,
            optMetricRegistry,
            tracer
          ).doFilter(wrappedRequest, wrappedResponse)

          logger.trace("{} -- Successfully processed request", nodeId)
        } catch {
          // Handle Throwables that arose while processing
          case ime: InvalidMethodException =>
            logger.debug(s"$nodeId -- Invalid HTTP method requested: ${wrappedRequest.getMethod}", ime)
            wrappedResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing request", true)
          case use: URISyntaxException =>
            logger.debug(s"$nodeId -- Invalid URI requested: ${wrappedRequest.getRequestURI}", use)
            wrappedResponse.sendError(SC_BAD_REQUEST, "Error processing request", true)
          case e: Exception =>
            logger.error(s"$nodeId -- Issue encountered while processing filter chain.", e)
            wrappedResponse.sendError(SC_BAD_GATEWAY, "Error processing request", true)
          case t: Throwable =>
            logger.error(s"$nodeId -- Error encountered while processing filter chain.", t)
            wrappedResponse.sendError(SC_BAD_GATEWAY, "Error processing request", true)
            throw t
        } finally {
          // Set the Via header
          if (wrappedResponse.isCommitted) {
            wrappedResponse.uncommit()
          }
          responseHeaderService.setVia(wrappedRequest, wrappedResponse)

          // Commit the response and record metrics for the request and response
          wrappedResponse.commitToResponse()
          closeSpan(wrappedResponse, scope)
          optMetricRegistry.foreach { mr =>
            markResponseCodeHelper(
              mr,
              wrappedResponse.getStatus,
              System.currentTimeMillis - startTime,
              logger.underlying)
          }

          logger.trace("ReposeFilter returning response...")
          MDC.clear()
        }

      case None =>
        logger.error("ReposeFilter has not yet initialized...")
        response.sendError(SC_INTERNAL_SERVER_ERROR, "ReposeFilter not initialized")
    }
  }
}

object ReposeFilter {
  private final val TraceIdLogger = LoggerFactory.getLogger(s"${classOf[ReposeFilter].getName}.trace-id-logging")
  private final val SupportedHttpMethods = Set(
    HttpGet.METHOD_NAME,
    HttpPut.METHOD_NAME,
    HttpPost.METHOD_NAME,
    HttpDelete.METHOD_NAME,
    HttpHead.METHOD_NAME,
    HttpOptions.METHOD_NAME,
    HttpPatch.METHOD_NAME,
    HttpTrace.METHOD_NAME)

  def markResponseCodeHelper(metricRegistry: MetricRegistry, responseCode: Int, lengthInMillis: Long, logger: Logger): Unit = {
    if (100 <= responseCode && responseCode < 600) {
      val statusCodeClass = "%dXX".format(responseCode / 100)
      metricRegistry.meter(
        MetricRegistry.name("org.openrepose.core.ResponseCode", "Repose", statusCodeClass)
      ).mark()
      metricRegistry.timer(
        MetricRegistry.name("org.openrepose.core.ResponseTime", "Repose", statusCodeClass)
      ).update(lengthInMillis, TimeUnit.MILLISECONDS)
    } else {
      logger.error(s"Repose: Encountered invalid response code: $responseCode")
    }
  }
}
