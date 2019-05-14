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

  // TODO: Are these the right keys? Use the single-character style keys?
  private final val Generators: Map[String, HttpLoggingContext => Any] = Map(
    "inboundRequestProtocol" -> (_.getInboundRequest.getProtocol),
    "outboundRequestProtocol" -> (_.getOutboundRequest.getProtocol),
    "inboundResponseProtocol" -> (_.getInboundResponseProtocol),
    "inboundRequestMethod" -> (_.getInboundRequest.getMethod),
    "outboundRequestMethod" -> (_.getOutboundRequest.getMethod),
    "inboundRequestPath" -> (_.getInboundRequest.getRequestURI),
    "outboundRequestPath" -> (_.getOutboundRequest.getRequestURI),
    "inboundRequestQueryString" -> (_.getInboundRequest.getQueryString),
    "outboundRequestQueryString" -> (_.getOutboundRequest.getQueryString),
    "inboundRequestUrl" -> (_.getInboundRequest.getRequestURL),
    "outboundRequestUrl" -> (_.getOutboundRequest.getRequestURL),
    "inboundRequestHeaders" -> (ctx => headerMap(ctx.getInboundRequest)),
    "outboundRequestHeaders" -> (ctx => headerMap(ctx.getOutboundRequest)),
    "outboundResponseHeaders" -> (ctx => headerMap(ctx.getOutboundResponse)),
    "outboundResponseStatusCode" -> (_.getOutboundResponse.getStatus),
    "outboundResponseReasonPhrase" -> (_.getOutboundResponseReasonPhrase),
    "timeRequestReceived" -> (_.getTimeRequestReceived),
    "timeToHandleRequest" -> (ctx => Duration.between(ctx.getTimeRequestReceived, ctx.getTimeRequestCompleted)),
    "timeInOriginService" -> (_.getTimeInOriginService),
    "localIpAddress" -> (_.getInboundRequest.getLocalAddr),
    "remoteIpAddress" -> (_.getInboundRequest.getRemoteAddr),
    "remoteHost" -> (_.getInboundRequest.getRemoteHost),
    "traceId" -> (ctx => TracingHeaderHelper.getTraceGuid(ctx.getOutboundRequest.getHeader(CommonHttpHeader.TRACE_GUID))),
    "extensions" -> (_.getExtensions)
  )

  def from(httpLoggingContext: HttpLoggingContext): Map[String, AnyRef] = {
    // Removes mappings where a value could not be generated.
    Generators
      .mapValues(generator => Try(generator(httpLoggingContext)))
      .mapValues(tryValue => tryValue.map(Option.apply).getOrElse(None))
      .collect({ case (key, Some(value: AnyRef)) => key -> value })
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
