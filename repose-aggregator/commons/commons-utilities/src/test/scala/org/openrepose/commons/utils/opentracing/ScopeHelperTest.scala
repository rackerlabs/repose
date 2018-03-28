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
import io.opentracing.propagation.Format
import io.opentracing.tag.Tags
import io.opentracing.{Scope, Span, SpanContext, Tracer}
import javax.servlet.http.HttpServletResponse
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyBoolean, anyString, isNull, eq => eql}
import org.mockito.Mockito.{verify, when}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.Logger
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class ScopeHelperTest extends FunSpec with MockitoSugar with Matchers {

  describe("startSpan") {
    it("should create a new span with the existing span as a parent") {
      val request = new MockHttpServletRequest()
      val tracer = mock[Tracer]
      val logger = mock[Logger]
      val spanContext = mock[SpanContext]
      val spanBuilder = mock[SpanBuilder]
      val scope = mock[Scope]

      when(tracer.extract(any[Format[_]], any())).thenReturn(spanContext)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.startActive(anyBoolean())).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_CLIENT, "1.two.III")

      verify(tracer).extract(eql(Format.Builtin.HTTP_HEADERS), any())
      verify(tracer).buildSpan(anyString())
      verify(spanBuilder).asChildOf(spanContext)
      verify(spanBuilder).startActive(true)
      result shouldBe scope
    }

    it("should create a new span with no parent if no parent context exists") {
      val request = new MockHttpServletRequest()
      val tracer = mock[Tracer]
      val logger = mock[Logger]
      val spanBuilder = mock[SpanBuilder]
      val scope = mock[Scope]

      when(tracer.extract(any[Format[_]], any())).thenReturn(null)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.startActive(anyBoolean())).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_CLIENT, "1.two.III")

      verify(tracer).extract(eql(Format.Builtin.HTTP_HEADERS), any())
      verify(tracer).buildSpan(anyString())
      verify(spanBuilder).asChildOf(isNull(classOf[SpanContext]))
      verify(spanBuilder).startActive(true)
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

      when(tracer.extract(any[Format[_]], any())).thenReturn(spanContext)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.startActive(anyBoolean())).thenReturn(scope)

      request.setMethod(method)
      request.setRequestURI(path)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_CLIENT, "1.two.III")

      verify(tracer).buildSpan(s"$method $path")
      result shouldBe scope
    }

    it("should set the span kind tag") {
      val spanKind = Tags.SPAN_KIND_PRODUCER
      val request = new MockHttpServletRequest()
      val tracer = mock[Tracer]
      val logger = mock[Logger]
      val spanBuilder = mock[SpanBuilder]
      val scope = mock[Scope]

      when(tracer.extract(any[Format[_]], any())).thenReturn(null)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.startActive(anyBoolean())).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, spanKind, "1.two.III")

      verify(spanBuilder).withTag(Tags.SPAN_KIND.getKey, spanKind)
      result shouldBe scope
    }

    it("should set the repose version tag") {
      val request = new MockHttpServletRequest()
      val tracer = mock[Tracer]
      val logger = mock[Logger]
      val spanBuilder = mock[SpanBuilder]
      val scope = mock[Scope]

      when(tracer.extract(any[Format[_]], any())).thenReturn(null)
      when(tracer.buildSpan(anyString())).thenReturn(spanBuilder)
      when(spanBuilder.asChildOf(any[SpanContext])).thenReturn(spanBuilder)
      when(spanBuilder.withTag(anyString(), anyString())).thenReturn(spanBuilder)
      when(spanBuilder.startActive(anyBoolean())).thenReturn(scope)

      val result = ScopeHelper.startSpan(request, tracer, logger, Tags.SPAN_KIND_PRODUCER, "1.two.III")

      verify(spanBuilder).withTag(ReposeTags.ReposeVersion, "1.two.III")
      result shouldBe scope
    }
  }

  describe("closeSpan") {
    it("should set the HTTP status tag") {
      val status = HttpServletResponse.SC_EXPECTATION_FAILED
      val response = new MockHttpServletResponse()
      val scope = mock[Scope]
      val span = mock[Span]

      response.setStatus(status)
      when(scope.span()).thenReturn(span)

      ScopeHelper.closeSpan(response, scope)

      verify(span).setTag(Tags.HTTP_STATUS.getKey, status)
    }

    it("should close the span") {
      val response = new MockHttpServletResponse()
      val scope = mock[Scope]

      when(scope.span()).thenReturn(mock[Span])

      ScopeHelper.closeSpan(response, scope)

      verify(scope).close()
    }
  }
}
