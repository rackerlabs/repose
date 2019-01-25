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
package org.openrepose.commons.utils.opentracing.httpclient

import java.io.IOException

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.opentracing.Tracer
import io.opentracing.propagation.Format.Builtin.HTTP_HEADERS
import io.opentracing.propagation.TextMap
import io.opentracing.tag.Tags._
import org.apache.http._
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HttpContext
import org.openrepose.commons.utils.http.CommonHttpHeader.{REQUEST_ID, VIA}
import org.openrepose.commons.utils.opentracing.ReposeTags.ReposeVersion
import org.openrepose.commons.utils.opentracing.httpclient.ReposeTracingInterceptorConstants.OpenTracingSpan
import org.openrepose.core.services.uriredaction.UriRedactionService

/**
  * An [[org.apache.http.HttpRequestInterceptor]] that will enrich Repose HTTP requests made through an
  * [[org.apache.http.client.HttpClient]] with OpenTracing data.
  *
  * This is based on the old [[com.uber.jaeger.httpclient.TracingRequestInterceptor]] since it was recently removed.
  * Additionally the old [[com.uber.jaeger.httpclient.SpanCreationRequestInterceptor]] and
  * [[com.uber.jaeger.httpclient.SpanInjectionRequestInterceptor]] were collapsed inline.
  *
  * @param tracer a [[io.opentracing.Tracer]] to bridge this utility with the OpenTracing API
  */
class ReposeTracingRequestInterceptor(tracer: Tracer, reposeVersion: String, uriRedactionService: UriRedactionService) extends HttpRequestInterceptor with StrictLogging {

  @throws[HttpException]
  @throws[IOException]
  override def process(httpRequest: HttpRequest, httpContext: HttpContext): Unit = {
    try {
      val currentActiveSpan = tracer.activeSpan
      val requestLine = httpRequest.getRequestLine
      val redactedUri = uriRedactionService.redact(requestLine.getUri)

      val clientSpanBuilder = tracer.buildSpan(s"${requestLine.getMethod} $redactedUri")
      Option(currentActiveSpan).foreach(clientSpanBuilder.asChildOf)

      val clientSpan = clientSpanBuilder.start

      clientSpan.setTag(SPAN_KIND.toString, SPAN_KIND_CLIENT)
      clientSpan.setTag(HTTP_URL.toString, redactedUri)

      httpContext match {
        case context: HttpClientContext =>
          val host = context.getTargetHost
          clientSpan.setTag(PEER_HOSTNAME.toString, host.getHostName)
          clientSpan.setTag(PEER_PORT.toString, host.getPort)
        case _ =>
      }

      Option(httpRequest.getFirstHeader(REQUEST_ID))
        .map(_.getValue)
        .foreach(clientSpan.setTag(REQUEST_ID, _))
      Option(httpRequest.getFirstHeader(VIA))
        .map(_.getValue)
        .foreach(clientSpan.setTag(VIA, _))
      clientSpan.setTag(ReposeVersion, reposeVersion)

      httpContext.setAttribute(OpenTracingSpan, clientSpan)

      tracer.inject(clientSpan.context, HTTP_HEADERS, new TextMap {
        override def iterator = throw new UnsupportedOperationException

        override def put(key: String, value: String): Unit = httpRequest.setHeader(new BasicHeader(key, value))
      })
    } catch {
      case e: Exception =>
        logger.error("Could not start client tracing span.", e)
    }
  }
}
