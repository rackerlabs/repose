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

import io.opentracing.Span
import io.opentracing.tag.Tags.HTTP_STATUS
import javax.servlet.http.HttpServletResponse.SC_OK
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpResponse, StatusLine}
import org.junit.runner.RunWith
import org.mockito.Matchers.{anyInt, eq => isEq}
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.utils.opentracing.httpclient.ReposeTracingInterceptorConstants.OpenTracingSpan
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeTracingResponseInterceptorTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  var statusLine: StatusLine = _
  var httpResponse: HttpResponse = _
  var httpContext: HttpContext = _
  var span: Span = _

  override def beforeEach(): Unit = {
    statusLine = mock[StatusLine]
    httpResponse = mock[HttpResponse]
    httpContext = mock[HttpContext]
    span = mock[Span]

    when(statusLine.getStatusCode).thenReturn(SC_OK)
    when(httpResponse.getStatusLine).thenReturn(statusLine)
  }

  describe("testProcess") {
    it("happy path") {
      when(httpContext.getAttribute(OpenTracingSpan)).thenReturn(span, Nil: _*)

      val httpResponseInterceptor = new ReposeTracingResponseInterceptor()

      httpResponseInterceptor.process(httpResponse, httpContext)

      verify(span).setTag(HTTP_STATUS.toString, SC_OK)
      verify(span).finish()
    }

    it("null span") {
      when(httpContext.getAttribute(OpenTracingSpan)).thenReturn(null, Nil: _*)

      val httpResponseInterceptor = new ReposeTracingResponseInterceptor()

      httpResponseInterceptor.process(httpResponse, httpContext)

      verify(span, never()).setTag(isEq(HTTP_STATUS.toString), anyInt)
    }

    it("span exception") {
      when(httpContext.getAttribute(OpenTracingSpan)).thenThrow(classOf[Exception])

      val httpResponseInterceptor = new ReposeTracingResponseInterceptor()

      httpResponseInterceptor.process(httpResponse, httpContext)

      verify(span, never()).setTag(isEq(HTTP_STATUS.toString), anyInt)
    }
  }
}
