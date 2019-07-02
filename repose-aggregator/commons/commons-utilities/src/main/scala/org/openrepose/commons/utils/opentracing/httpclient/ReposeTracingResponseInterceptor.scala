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

import com.typesafe.scalalogging.StrictLogging
import io.opentracing.Span
import io.opentracing.tag.Tags._
import org.apache.http._
import org.apache.http.protocol.HttpContext
import org.openrepose.commons.utils.opentracing.httpclient.ReposeTracingInterceptorConstants.OpenTracingSpan

/**
  * An [[org.apache.http.HttpResponseInterceptor]] that will enrich Repose HTTP response made through an
  * [[org.apache.http.client.HttpClient]] with OpenTracing data.
  *
  * This is based on the old [[com.uber.jaeger.httpclient.TracingResponseInterceptor]] since it was recently removed.
  */
class ReposeTracingResponseInterceptor extends HttpResponseInterceptor with StrictLogging {
  @throws[HttpException]
  @throws[IOException]
  override def process(httpResponse: HttpResponse, httpContext: HttpContext): Unit = {
    try {
      Option(httpContext.getAttribute(OpenTracingSpan).asInstanceOf[Span]) match {
        case Some(span: Span) =>
          span.setTag(HTTP_STATUS.toString, httpResponse.getStatusLine.getStatusCode)
          span.finish()
        case _ =>
          logger.warn("The ResponseInterceptor did not find a clientSpan. Verify that the RequestInterceptor is correctly set up.")
      }
    } catch {
      case e: Exception =>
        logger.error("Could not finish client tracing span.", e)
    }
  }
}
