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
package org.openrepose.core.services.opentracing.interceptors

import com.uber.jaeger.httpclient.TracingRequestInterceptor
import io.opentracing.{Span, Tracer}
import org.apache.http.{HttpRequest}
import org.apache.http.protocol.HttpContext
import org.openrepose.commons.utils.http.CommonHttpHeader

class JaegerRequestInterceptor(tracer: Tracer) extends TracingRequestInterceptor(tracer)
  with RequestInterceptor {

  override protected def onSpanStarted(clientSpan: Span, httpRequest: HttpRequest, httpContext: HttpContext): Unit = {
    val traceRequestHeader = httpRequest.getFirstHeader(CommonHttpHeader.REQUEST_ID)
    val viaRequestHeader = httpRequest.getFirstHeader(CommonHttpHeader.VIA)
    if (traceRequestHeader != null) clientSpan.setTag(CommonHttpHeader.REQUEST_ID, traceRequestHeader.getValue)
    if (viaRequestHeader != null) clientSpan.setTag(CommonHttpHeader.VIA, viaRequestHeader.getValue)
    super.onSpanStarted(clientSpan, httpRequest, httpContext)
  }

  override protected def getOperationName(httpRequest: HttpRequest): String =
    s"${httpRequest.getRequestLine.getMethod} ${httpRequest.getRequestLine.getUri}"


}
