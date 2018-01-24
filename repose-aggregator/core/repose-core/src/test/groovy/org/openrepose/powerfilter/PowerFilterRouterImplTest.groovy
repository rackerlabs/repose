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
package org.openrepose.powerfilter

import akka.actor.dsl.Creators
import io.opentracing.ActiveSpan
import io.opentracing.BaseSpan
import io.opentracing.Span
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.mock.MockTracer
import io.opentracing.propagation.Format
import io.opentracing.tag.Tags
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.routing.DestinationLocation
import org.openrepose.core.filter.routing.DestinationLocationBuilder
import org.openrepose.core.services.opentracing.OpenTracingService
import org.openrepose.core.services.opentracing.TraceGUIDInjector
import org.openrepose.core.services.reporting.ReportingService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.systemmodel.config.Destination
import org.openrepose.core.systemmodel.config.DestinationEndpoint
import org.openrepose.core.systemmodel.config.ReposeCluster
import org.openrepose.nodeservice.request.RequestHeaderService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockRequestDispatcher
import org.springframework.mock.web.MockServletContext
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.rmi.server.UnicastRemoteObject

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.Mockito.times

class PowerFilterRouterImplTest extends Specification {

    PowerFilterRouterImpl powerFilterRouter

    DestinationLocationBuilder destinationLocationBuilder = mock(DestinationLocationBuilder.class)
    ReposeCluster reposeCluster = mock(ReposeCluster.class)
    ServletContext servletContext = mock(ServletContext.class)
    RequestHeaderService requestHeaderService = mock(RequestHeaderService.class)
    ResponseHeaderService responseHeaderService = mock(ResponseHeaderService.class)
    ReportingService reportingService = mock(ReportingService.class)
    Optional<MetricsService> metricsService = Optional.empty()
    Optional<OpenTracingService> openTracingService = Optional.of(mock(OpenTracingService.class))

    @Unroll
    def "route destinations: #destinationMap and default destination: #defaultDestination"() {
        given:
        powerFilterRouter = new PowerFilterRouterImpl(destinationLocationBuilder,
            destinationMap,
            reposeCluster,
            defaultDestination,
            servletContext,
            requestHeaderService,
            responseHeaderService,
            reportingService,
            metricsService,
            openTracingService
        )

        HttpServletRequestWrapper httpServletRequestWrapper = mock(HttpServletRequestWrapper.class)
        HttpServletResponse httpServletResponse = new MockHttpServletResponse()

        ServletContext targetServletContext = mock(ServletContext.class)
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class)

        when(servletContext.getContext(anyString())).thenReturn(targetServletContext)
        when(targetServletContext.getRequestDispatcher(anyString())).thenReturn(requestDispatcher)

        when:
        powerFilterRouter.route(httpServletRequestWrapper, httpServletResponse)

        then:
        assert httpServletResponse.getStatus() == responseCode
        verify(requestDispatcher, never()).forward(
            any(ServletRequest.class), any(ServletResponse.class))
        verify(reportingService, never()).incrementRequestCount(anyString())

        where:
        destinationMap  | defaultDestination | responseCode
        new HashMap<>() | "/"                | HttpServletResponse.SC_NOT_FOUND
        new HashMap<>() | null               | HttpServletResponse.SC_OK

    }

    @Unroll
    def "route with OpenTracing service disabled: default destination: #defaultDestination, destination id: #destinationId, destination default flag: #destinationDefault"() {
        given:
        Map<String, Destination> destinationMap = new HashMap<>()
        Destination destination = new DestinationEndpoint()
        destination.setDefault(destinationDefault)
        destination.setId(destinationId)
        destination.setProtocol("http")
        destination.setRootPath("/")
        destinationMap.put("/", destination)
        powerFilterRouter = new PowerFilterRouterImpl(destinationLocationBuilder,
            destinationMap,
            reposeCluster,
            defaultDestination,
            servletContext,
            requestHeaderService,
            responseHeaderService,
            reportingService,
            metricsService,
            openTracingService
        )

        HttpServletRequestWrapper httpServletRequestWrapper = mock(HttpServletRequestWrapper.class)
        HttpServletResponse httpServletResponse = new MockHttpServletResponse()

        ServletContext targetServletContext = mock(ServletContext.class)
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class)

        when(servletContext.getContext(anyString())).thenReturn(targetServletContext)
        when(targetServletContext.getRequestDispatcher(anyString())).thenReturn(requestDispatcher)
        when(targetServletContext.getContextPath()).thenReturn("")

        // populate that location
        when(destinationLocationBuilder.build(any(), anyString(), any(HttpServletRequest.class))).thenReturn(
            new DestinationLocation(
                new URL(destination.getProtocol(), "localhost", 8080, destination.getRootPath()),
                new URI("http", null, "localhost", 8080, destination.getRootPath(),
                    "", null)
            )
        )

        when:
        powerFilterRouter.route(httpServletRequestWrapper, httpServletResponse)

        then:
        assert httpServletResponse.getStatus() == responseCode
        assert openTracingService.isPresent() == true
        assert openTracingService.get().isEnabled() == false

        if (defaultDestination == null) {
            verify(requestDispatcher, never()).forward(
                any(ServletRequest.class), any(ServletResponse.class))
            verify(reportingService, never()).incrementRequestCount(anyString())

        } else {
            verify(requestDispatcher, times(1)).forward(
                any(ServletRequest.class), any(ServletResponse.class))
            verify(reportingService, times(1)).incrementRequestCount(anyString())
        }

        where:

        defaultDestination | destinationId | destinationDefault | responseCode
        "/"                | "test"        | true               | HttpServletResponse.SC_OK
        null               | "test"        | true               | HttpServletResponse.SC_OK
        "/"                | "test"        | false              | HttpServletResponse.SC_OK
        null               | "test"        | false              | HttpServletResponse.SC_OK
        "/"                | null          | true               | HttpServletResponse.SC_OK
        null               | null          | true               | HttpServletResponse.SC_OK
        "/"                | null          | false              | HttpServletResponse.SC_OK
        null               | null          | false              | HttpServletResponse.SC_OK

    }

    @Unroll
    def "route with OpenTracing service enabled: default destination: #defaultDestination, destination id: #destinationId, destination default flag: #destinationDefault"() {
        given:
        Map<String, Destination> destinationMap = new HashMap<>()
        Destination destination = new DestinationEndpoint()
        destination.setDefault(destinationDefault)
        destination.setId(destinationId)
        destination.setProtocol("http")
        destination.setRootPath("/")
        destinationMap.put("/", destination)
        powerFilterRouter = new PowerFilterRouterImpl(destinationLocationBuilder,
            destinationMap,
            reposeCluster,
            defaultDestination,
            servletContext,
            requestHeaderService,
            responseHeaderService,
            reportingService,
            metricsService,
            openTracingService
        )

        HttpServletRequestWrapper httpServletRequestWrapper = mock(HttpServletRequestWrapper.class)
        HttpServletResponse httpServletResponse = new MockHttpServletResponse()

        ServletContext targetServletContext = mock(ServletContext.class)
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class)

        when(servletContext.getContext(anyString())).thenReturn(targetServletContext)
        when(targetServletContext.getRequestDispatcher(anyString())).thenReturn(requestDispatcher)
        when(targetServletContext.getContextPath()).thenReturn("")
        when(httpServletRequestWrapper.getMethod()).thenReturn("POST")
        when(httpServletRequestWrapper.getRequestURL()).thenReturn(new StringBuffer().append("http://localhost"))

        // populate that location
        when(destinationLocationBuilder.build(any(), anyString(), any(HttpServletRequest.class))).thenReturn(
            new DestinationLocation(
                new URL(destination.getProtocol(), "localhost", 8080, destination.getRootPath()),
                new URI("http", null, "localhost", 8080, destination.getRootPath(),
                    "", null)
            )
        )

        if (dispatchError != null ) {
            when(requestDispatcher.forward(any(ServletRequest.class), any(ServletResponse.class))).thenThrow(dispatchError)
        }

        // OpenTracing
        Tracer mockTracer = mock(Tracer.class)
        ActiveSpan mockActiveSpan = mock(ActiveSpan.class)

        ActiveSpan childSpan = mock(ActiveSpan.class)
        Tracer.SpanBuilder spanBuilderMock = new FakeSpanBuilder("test", childSpan)

        when(mockTracer.activeSpan()).thenReturn(mockActiveSpan)
        when(mockTracer.buildSpan(anyString())).thenReturn(spanBuilderMock)

        when(openTracingService.get().isEnabled()).thenReturn(true)
        when(openTracingService.get().getGlobalTracer()).thenReturn(mockTracer)

        when:
        powerFilterRouter.route(httpServletRequestWrapper, httpServletResponse)

        then:
        assert httpServletResponse.getStatus() == responseCode
        assert openTracingService.isPresent() == true
        assert openTracingService.get().isEnabled() == true

        if (defaultDestination == null) {
            verify(requestDispatcher, never()).forward(
                any(ServletRequest.class), any(ServletResponse.class))
            verify(reportingService, never()).incrementRequestCount(anyString())

            verify(mockTracer, never()).buildSpan(anyString())
            verify(mockTracer, never()).inject(
                any(SpanContext.class), any(Format.class), any(TraceGUIDInjector.class))
            verify(childSpan, never()).close()
        } else {
            verify(requestDispatcher, times(1)).forward(
                any(ServletRequest.class), any(ServletResponse.class))
            verify(reportingService, times(1)).incrementRequestCount(anyString())

            verify(mockTracer, times(1)).buildSpan(anyString())
            verify(mockTracer, times(1)).inject(
                any(SpanContext.class), any(Format.class), any(TraceGUIDInjector.class))

            verify(childSpan, times(1)).close()
            verify(childSpan, times(1)).setTag(anyString(), anyInt())
        }

        where:

        defaultDestination | destinationId | destinationDefault | responseCode                | dispatchError
        "/"                | "test"        | true               | HttpServletResponse.SC_OK                         | null
        null               | "test"        | true               | HttpServletResponse.SC_OK                         | null
        "/"                | "test"        | false              | HttpServletResponse.SC_OK                         | null
        null               | "test"        | false              | HttpServletResponse.SC_OK                         | null
        "/"                | null          | true               | HttpServletResponse.SC_OK                         | null
        null               | null          | true               | HttpServletResponse.SC_OK                         | null
        "/"                | null          | false              | HttpServletResponse.SC_OK                         | null
        null               | null          | false              | HttpServletResponse.SC_OK                         | null
        "/"                | "test"        | true               | HttpServletResponse.SC_SERVICE_UNAVAILABLE        | new IOException()
        null               | "test"        | true               | HttpServletResponse.SC_OK                         | new IOException()
        "/"                | "test"        | false              | HttpServletResponse.SC_SERVICE_UNAVAILABLE        | new IOException()
        null               | "test"        | false              | HttpServletResponse.SC_OK                         | new IOException()
        "/"                | null          | true               | HttpServletResponse.SC_SERVICE_UNAVAILABLE        | new IOException()
        null               | null          | true               | HttpServletResponse.SC_OK                         | new IOException()
        "/"                | null          | false              | HttpServletResponse.SC_SERVICE_UNAVAILABLE        | new IOException()
        null               | null          | false              | HttpServletResponse.SC_OK                         | new IOException()
        "/"                | "test"        | true               | HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE   | new IOException().initCause(new ReadLimitReachedException("too much"))
        null               | "test"        | true               | HttpServletResponse.SC_OK                         | new IOException().initCause(new ReadLimitReachedException("too much"))
        "/"                | "test"        | false              | HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE   | new IOException().initCause(new ReadLimitReachedException("too much"))
        null               | "test"        | false              | HttpServletResponse.SC_OK                         | new IOException().initCause(new ReadLimitReachedException("too much"))
        "/"                | null          | true               | HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE   | new IOException().initCause(new ReadLimitReachedException("too much"))
        null               | null          | true               | HttpServletResponse.SC_OK                         | new IOException().initCause(new ReadLimitReachedException("too much"))
        "/"                | null          | false              | HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE   | new IOException().initCause(new ReadLimitReachedException("too much"))
        null               | null          | false              | HttpServletResponse.SC_OK                         | new IOException().initCause(new ReadLimitReachedException("too much"))

    }


    class FakeSpanBuilder implements Tracer.SpanBuilder {

        String operationName
        ActiveSpan activeSpan

        FakeSpanBuilder(operationName, activeSpan) {
            this.operationName = operationName
            this.activeSpan = activeSpan
        }

        @Override
        Tracer.SpanBuilder asChildOf(SpanContext parent) {
            return this
        }

        @Override
        Tracer.SpanBuilder asChildOf(BaseSpan<?> parent) {
            return this
        }

        @Override
        Tracer.SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            return null
        }

        @Override
        Tracer.SpanBuilder ignoreActiveSpan() {
            return null
        }

        @Override
        Tracer.SpanBuilder withTag(String key, String value) {
            return this
        }

        @Override
        Tracer.SpanBuilder withTag(String key, boolean value) {
            return this
        }

        @Override
        Tracer.SpanBuilder withTag(String key, Number value) {
            return null
        }

        @Override
        Tracer.SpanBuilder withStartTimestamp(long microseconds) {
            return this
        }

        @Override
        ActiveSpan startActive() {
            return this.activeSpan
        }

        @Override
        Span startManual() {
            return null
        }

        @Override
        Span start() {
            return null
        }
    }
}
