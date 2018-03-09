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

import io.opentracing.{Scope, ScopeManager, Span, Tracer}
import org.apache.http.HttpRequest
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HttpContext
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeTracingRequestInterceptorTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  describe("testOnSpanStarted") {
    it("no headers") {
      val httpRequest = mock[HttpRequest]
      val httpContext = mock[HttpContext]
      val tracer = mock[Tracer]
      val scopeManager = mock[ScopeManager]
      val scope = mock[Scope]
      val span = mock[Span]

      when(scope.span()).thenReturn(span)
      when(scopeManager.active()).thenReturn(scope)
      when(tracer.scopeManager()).thenReturn(scopeManager)

      val tracingRequestInterceptor = new ReposeTracingRequestInterceptor(tracer)

      tracingRequestInterceptor.process(httpRequest, httpContext)

      verify(span, never()).setTag(any[String](), any[String]())
    }

    it("with request header") {
      val httpRequest = mock[HttpRequest]
      val httpContext = mock[HttpContext]
      val tracer = mock[Tracer]
      val scopeManager = mock[ScopeManager]
      val scope = mock[Scope]
      val span = mock[Span]

      when(httpRequest.getFirstHeader(any())).thenReturn(new BasicHeader(CommonHttpHeader.REQUEST_ID, "1234"))
      when(scope.span()).thenReturn(span)
      when(scopeManager.active()).thenReturn(scope)
      when(tracer.scopeManager()).thenReturn(scopeManager)

      val jaegerRequestInterceptor = new ReposeTracingRequestInterceptor(tracer)

      jaegerRequestInterceptor.process(httpRequest, httpContext)

      verify(span, times(1)).setTag(CommonHttpHeader.REQUEST_ID, "1234")
    }

    it("with via header") {
      val httpRequest = mock[HttpRequest]
      val httpContext = mock[HttpContext]
      val tracer = mock[Tracer]
      val scopeManager = mock[ScopeManager]
      val scope = mock[Scope]
      val span = mock[Span]

      when(httpRequest.getFirstHeader(any())).thenReturn(new BasicHeader(CommonHttpHeader.VIA, "1234"))
      when(scope.span()).thenReturn(span)
      when(scopeManager.active()).thenReturn(scope)
      when(tracer.scopeManager()).thenReturn(scopeManager)

      val jaegerRequestInterceptor = new ReposeTracingRequestInterceptor(tracer)

      jaegerRequestInterceptor.process(httpRequest, httpContext)

      verify(span, times(1)).setTag(CommonHttpHeader.VIA, "1234")
    }
  }

}
