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

import io.opentracing.Tracer.SpanBuilder
import io.opentracing.tag.Tags.HTTP_URL
import io.opentracing.{Span, Tracer}
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, RequestLine}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.CommonHttpHeader.{REQUEST_ID, VIA}
import org.openrepose.commons.utils.opentracing.ReposeTags.ReposeVersion
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeTracingRequestInterceptorTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  val redactedPath = "/its/redacted"
  var requestLine: RequestLine = _
  var httpRequest: HttpRequest = _
  var httpContext: HttpContext = _
  var span: Span = _
  var spanBuilder: SpanBuilder = _
  var tracer: Tracer = _
  var uriRedactionService: UriRedactionService = _

  override def beforeEach(): Unit = {
    requestLine = mock[RequestLine]
    httpRequest = mock[HttpRequest]
    httpContext = mock[HttpContext]
    span = mock[Span]
    spanBuilder = mock[SpanBuilder]
    tracer = mock[Tracer]
    uriRedactionService = mock[UriRedactionService]

    when(requestLine.getMethod).thenReturn("GET")
    when(requestLine.getUri).thenReturn("/redact/me")
    when(httpRequest.getRequestLine).thenReturn(requestLine)
    when(uriRedactionService.redact(requestLine.getUri)).thenReturn(redactedPath)
    when(spanBuilder.start()).thenReturn(span)
    when(tracer.buildSpan(s"${requestLine.getMethod} $redactedPath")).thenReturn(spanBuilder)
  }

  describe("testProcess") {
    it("no headers") {
      val httpRequestInterceptor = new ReposeTracingRequestInterceptor(tracer, "1.two.III", uriRedactionService)

      httpRequestInterceptor.process(httpRequest, httpContext)

      verify(span).setTag(ReposeVersion, "1.two.III")
      verify(uriRedactionService).redact(requestLine.getUri)
      verify(span).setTag(HTTP_URL.toString, redactedPath)
    }

    it("with request header") {
      when(httpRequest.getFirstHeader(any())).thenReturn(new BasicHeader(REQUEST_ID, "1234"))

      val httpRequestInterceptor = new ReposeTracingRequestInterceptor(tracer, "1.two.III", uriRedactionService)

      httpRequestInterceptor.process(httpRequest, httpContext)

      verify(span).setTag(REQUEST_ID, "1234")
      verify(uriRedactionService).redact(requestLine.getUri)
      verify(span).setTag(HTTP_URL.toString, redactedPath)
    }

    it("with via header") {
      when(httpRequest.getFirstHeader(any())).thenReturn(new BasicHeader(VIA, "1234"))

      val httpRequestInterceptor = new ReposeTracingRequestInterceptor(tracer, "1.two.III", uriRedactionService)

      httpRequestInterceptor.process(httpRequest, httpContext)

      verify(span).setTag(VIA, "1234")
      verify(uriRedactionService).redact(requestLine.getUri)
      verify(span).setTag(HTTP_URL.toString, redactedPath)
    }
  }
}
