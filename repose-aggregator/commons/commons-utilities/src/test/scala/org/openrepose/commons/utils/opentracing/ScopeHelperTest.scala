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
package org.openrepose.commons.utils.opentracing

import io.opentracing.Tracer.SpanBuilder
import io.opentracing._
import io.opentracing.propagation.Format
import io.opentracing.tag.Tags

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, isNull, eq => eql}
import org.mockito.Mockito.{verify, when}
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.slf4j.Logger
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class ScopeHelperTest extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterEach {

  val redactedPath = "/its/redacted"
  var request: HttpServletRequest = _
  var tracer: Tracer = _
  var logger: Logger = _
  var spanContext: SpanContext = _
  var spanBuilder: SpanBuilder = _
  var scope: Scope = _
  var span: Span = _
  var uriRedactionService: UriRedactionService = _

  override def beforeEach(): Unit = {
    request = new MockHttpServletRequest("GET", "/redact/me")
    tracer = mock[Tracer]
    logger = mock[Logger]
    spanContext = mock[SpanContext]
    spanBuilder = mock[SpanBuilder]
    scope = mock[Scope]
    span = mock[Span]
    uriRedactionService = mock[UriRedactionService]
    when(uriRedactionService.redact(request.getRequestURI)).thenReturn(redactedPath)
  }

  describe("startSpan") {
    it("should create a new span with the existing span as a parent") {
      when(tracer.extract(any[Format[_]], any())).thenReturn(spanContext)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.start()).thenReturn(span)
      when(tracer.activateSpan(any[Span])).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_CLIENT, "1.two.III", uriRedactionService)

      verify(tracer).extract(eql(Format.Builtin.HTTP_HEADERS), any())
      verify(tracer).buildSpan(s"${request.getMethod} $redactedPath")
      verify(spanBuilder).asChildOf(spanContext)
      verify(spanBuilder).start()
      verify(tracer).activateSpan(span)
      result shouldBe scope
    }

    it("should create a new span with no parent if no parent context exists") {
      when(tracer.extract(any[Format[_]], any())).thenReturn(null)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.start()).thenReturn(span)
      when(tracer.activateSpan(any[Span])).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_CLIENT, "1.two.III", uriRedactionService)

      verify(tracer).extract(eql(Format.Builtin.HTTP_HEADERS), any())
      verify(tracer).buildSpan(s"${request.getMethod} $redactedPath")
      verify(spanBuilder).asChildOf(isNull(classOf[SpanContext]))
      verify(spanBuilder).start()
      verify(tracer).activateSpan(span)
      result shouldBe scope
    }

    it("should set the operation name of the new span to the method and path") {
      val method = "POST"
      val path = "/post"
      val request = new MockHttpServletRequest()
      val tracer = mock[Tracer]
      val logger = mock[Logger]
      val spanContext = mock[SpanContext]
      val spanBuilder = mock[SpanBuilder]
      val scope = mock[Scope]

      request.setMethod(method)
      request.setRequestURI(path)

      when(tracer.extract(any[Format[_]], any())).thenReturn(spanContext)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.start()).thenReturn(span)
      when(tracer.activateSpan(any[Span])).thenReturn(scope)
      when(uriRedactionService.redact(request.getRequestURI)).thenReturn(redactedPath)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_CLIENT, "1.two.III", uriRedactionService)

      verify(tracer).buildSpan(s"$method $redactedPath")
      result shouldBe scope
    }

    it("should set the span kind tag") {
      val spanKind = Tags.SPAN_KIND_PRODUCER

      when(tracer.extract(any[Format[_]], any())).thenReturn(null)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.start()).thenReturn(span)
      when(tracer.activateSpan(any[Span])).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, spanKind, "1.two.III", uriRedactionService)

      verify(tracer).buildSpan(s"${request.getMethod} $redactedPath")
      verify(spanBuilder).withTag(Tags.SPAN_KIND.getKey, spanKind)
      result shouldBe scope
    }

    it("should set the repose version tag") {
      when(tracer.extract(any[Format[_]], any())).thenReturn(null)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.start()).thenReturn(span)
      when(tracer.activateSpan(any[Span])).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_PRODUCER, "1.two.III", uriRedactionService)

      verify(tracer).buildSpan(s"${request.getMethod} $redactedPath")
      verify(spanBuilder).withTag(ReposeTags.ReposeVersion, "1.two.III")
      result shouldBe scope
    }
  }

  describe("closeSpan") {
    it("should set the HTTP status tag") {
      val status = HttpServletResponse.SC_EXPECTATION_FAILED
      val response = new MockHttpServletResponse()
      val tracer = mock[Tracer]
      val scope = mock[Scope]
      val span = mock[Span]
      val scopeManager = mock[ScopeManager]

      response.setStatus(status)
      when(tracer.activeSpan()).thenReturn(span)

      ScopeHelper.closeSpan(response, scopeManager, scope)

      verify(span).setTag(Tags.HTTP_STATUS.getKey, status)
    }

    it("should close the span") {
      val response = new MockHttpServletResponse()
      val scope = mock[Scope]
      val span = mock[Span]
      val scopeManager = mock[ScopeManager]

      when(scopeManager.activeSpan()).thenReturn(span)

      ScopeHelper.closeSpan(response, scopeManager, scope)

      verify(scope).close()
    }
  }
}
