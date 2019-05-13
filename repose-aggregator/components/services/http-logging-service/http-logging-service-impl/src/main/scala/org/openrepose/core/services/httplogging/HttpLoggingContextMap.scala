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
package org.openrepose.core.services.httplogging

import java.time.Duration
import java.util

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.Try

/**
  * Converts an [[HttpLoggingContext]] into a [[Map]] for use by the templating
  * engine.
  */
object HttpLoggingContextMap {

  private final type Mapping = (String, Any)

  // Handles exceptions thrown by mappings.
  // Doing so enables a map to be created with values generated in a predetermined, but unreliable, manner.
  private final implicit def optionalMapping(mapping: => Mapping): Option[Mapping] = {
    Try(mapping)
      .toOption
      .filter(Function.tupled((_, value) => Option(value).isDefined))
  }

  def from(httpLoggingContext: HttpLoggingContext): Map[String, AnyRef] = {
    val mappings: Seq[Option[Mapping]] = Seq(
      "inboundRequestProtocol" -> httpLoggingContext.getInboundRequest.getProtocol,
      "outboundRequestProtocol" -> httpLoggingContext.getOutboundRequest.getProtocol,
      "inboundRequestMethod" -> httpLoggingContext.getInboundRequest.getMethod,
      "outboundRequestMethod" -> httpLoggingContext.getOutboundRequest.getMethod,
      "inboundRequestPath" -> httpLoggingContext.getInboundRequest.getRequestURI,
      "outboundRequestPath" -> httpLoggingContext.getOutboundRequest.getRequestURI,
      "inboundRequestQueryString" -> httpLoggingContext.getInboundRequest.getQueryString,
      "outboundRequestQueryString" -> httpLoggingContext.getOutboundRequest.getQueryString,
      "inboundRequestUrl" -> httpLoggingContext.getInboundRequest.getRequestURL,
      "outboundRequestUrl" -> httpLoggingContext.getOutboundRequest.getRequestURL,
      "inboundRequestHeaders" -> headerMap(httpLoggingContext.getInboundRequest),
      "outboundRequestHeaders" -> headerMap(httpLoggingContext.getOutboundRequest),
      "outboundResponseHeaders" -> headerMap(httpLoggingContext.getOutboundResponse),
      "outboundResponseStatusCode" -> httpLoggingContext.getOutboundResponse.getStatus,
      "outboundResponseReasonPhrase" -> httpLoggingContext.getOutboundResponseReasonPhrase,
      "timeRequestReceived" -> httpLoggingContext.getTimeRequestReceived,
      "timeToHandleRequest" -> Duration.between(httpLoggingContext.getTimeRequestReceived, httpLoggingContext.getTimeRequestCompleted),
      "timeInOriginService" -> httpLoggingContext.getTimeInOriginService,
      "localIpAddress" -> httpLoggingContext.getInboundRequest.getLocalAddr,
      "remoteIpAddress" -> httpLoggingContext.getInboundRequest.getRemoteAddr,
      "remoteHost" -> httpLoggingContext.getInboundRequest.getRemoteHost,
      "traceId" -> TracingHeaderHelper.getTraceGuid(httpLoggingContext.getOutboundRequest.getHeader(CommonHttpHeader.TRACE_GUID)),
      "extensions" -> httpLoggingContext.getExtensions
    )

    // Removes mappings where a value could not be generated, then restructures the data.
    mappings
      .flatten
      .toMap
      .mapValues(_.asInstanceOf[AnyRef])
  }

  private def headerMap(request: HttpServletRequest): util.Map[String, _ <: util.List[String]] = {
    // Note that header names are lower-cased so that lookups are consistent.
    // Unfortunately, even if we pass JTwig a case-insensitive map, we see that
    // casing matters when rendering a template (implying that JTwig converts or
    // wraps our map with their own which is case-sensitive).
    request.getHeaderNames.asScala
      .map(headerName => headerName.toLowerCase -> util.Collections.list(request.getHeaders(headerName)))
      .toMap
      .asJava
  }

  private def headerMap(response: HttpServletResponse): util.Map[String, _ <: util.List[String]] = {
    // Note that header names are lower-cased so that lookups are consistent.
    // Unfortunately, even if we pass JTwig a case-insensitive map, we see that
    // casing matters when rendering a template (implying that JTwig converts or
    // wraps our map with their own which is case-sensitive).
    response.getHeaderNames.asScala
      .map(headerName => headerName.toLowerCase -> new util.ArrayList[String](response.getHeaders(headerName)))
      .toMap
      .asJava
  }
}
