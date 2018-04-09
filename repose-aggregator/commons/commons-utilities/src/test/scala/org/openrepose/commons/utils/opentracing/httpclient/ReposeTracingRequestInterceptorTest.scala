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
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.opentracing.ReposeTags
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeTracingRequestInterceptorTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  var httpRequest: HttpRequest = _
  var httpContext: HttpContext = _
  var tracer: Tracer = _
  var uriRedactionService: UriRedactionService = _
  var scopeManager: ScopeManager = _
  var scope: Scope = _
  var span: Span = _

  override def beforeEach(): Unit = {
    httpRequest = mock[HttpRequest]
    httpContext = mock[HttpContext]
    tracer = mock[Tracer]
    uriRedactionService = mock[UriRedactionService]
    scopeManager = mock[ScopeManager]
    scope = mock[Scope]
    span = mock[Span]
  }

  describe("testOnSpanStarted") {
    it("no headers") {
      when(scope.span()).thenReturn(span)
      when(scopeManager.active()).thenReturn(scope)
      when(tracer.scopeManager()).thenReturn(scopeManager)

      val tracingRequestInterceptor = new ReposeTracingRequestInterceptor(tracer, "1.two.III", uriRedactionService)

      tracingRequestInterceptor.process(httpRequest, httpContext)

      verify(span).setTag(ReposeTags.ReposeVersion, "1.two.III")
    }

    it("with request header") {
      when(httpRequest.getFirstHeader(any())).thenReturn(new BasicHeader(CommonHttpHeader.REQUEST_ID, "1234"))
      when(scope.span()).thenReturn(span)
      when(scopeManager.active()).thenReturn(scope)
      when(tracer.scopeManager()).thenReturn(scopeManager)

      val jaegerRequestInterceptor = new ReposeTracingRequestInterceptor(tracer, "1.two.III", uriRedactionService)

      jaegerRequestInterceptor.process(httpRequest, httpContext)

      verify(span).setTag(CommonHttpHeader.REQUEST_ID, "1234")
    }

    it("with via header") {
      when(httpRequest.getFirstHeader(any())).thenReturn(new BasicHeader(CommonHttpHeader.VIA, "1234"))
      when(scope.span()).thenReturn(span)
      when(scopeManager.active()).thenReturn(scope)
      when(tracer.scopeManager()).thenReturn(scopeManager)

      val jaegerRequestInterceptor = new ReposeTracingRequestInterceptor(tracer, "1.two.III", uriRedactionService)

      jaegerRequestInterceptor.process(httpRequest, httpContext)

      verify(span).setTag(CommonHttpHeader.VIA, "1234")
    }
  }
}
