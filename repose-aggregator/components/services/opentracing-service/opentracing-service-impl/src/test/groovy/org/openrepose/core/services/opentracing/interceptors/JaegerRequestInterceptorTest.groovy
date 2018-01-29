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

import io.opentracing.Span
import io.opentracing.Tracer
import org.apache.http.Header
import org.apache.http.HttpRequest
import org.apache.http.RequestLine
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HttpContext
import org.junit.Test
import org.openrepose.commons.utils.http.CommonHttpHeader

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.times
import static org.mockito.Mockito.when

import static org.junit.Assert.*


class JaegerRequestInterceptorTest {

    HttpRequest httpRequest = mock(HttpRequest.class)
    HttpContext httpContext = mock(HttpContext.class)
    Tracer tracer = mock(Tracer.class)
    Span span = mock(Span.class)

    @Test
    void testOnSpanStartedWithRequestHeader() {

        Header requestHeader = new BasicHeader(CommonHttpHeader.REQUEST_ID, "myvalue")
        when(httpRequest.getFirstHeader(CommonHttpHeader.REQUEST_ID)).thenReturn(requestHeader)

        JaegerRequestInterceptor jaegerRequestInterceptor = new JaegerRequestInterceptor(tracer)
        jaegerRequestInterceptor.onSpanStarted(span, httpRequest, httpContext)

        verify(span, times(1)).setTag(CommonHttpHeader.REQUEST_ID, "myvalue")
    }

    @Test
    void testOnSpanStartedNoRequestHeader() {

        JaegerRequestInterceptor jaegerRequestInterceptor = new JaegerRequestInterceptor(tracer)
        jaegerRequestInterceptor.onSpanStarted(span, httpRequest, httpContext)

        verify(span, never()).setTag(CommonHttpHeader.REQUEST_ID, "myvalue")
    }

    @Test
    void testGetOperationName() {
        RequestLine requestLine = mock(RequestLine.class)
        when(httpRequest.getRequestLine()).thenReturn(requestLine)
        when(requestLine.getMethod()).thenReturn("GET")
        when(requestLine.getUri()).thenReturn("/")

        JaegerRequestInterceptor jaegerRequestInterceptor = new JaegerRequestInterceptor(tracer)
        assertEquals("Validate that operation name is set to combination of method and uri",
            jaegerRequestInterceptor.getOperationName(httpRequest), "GET /")
    }
}
