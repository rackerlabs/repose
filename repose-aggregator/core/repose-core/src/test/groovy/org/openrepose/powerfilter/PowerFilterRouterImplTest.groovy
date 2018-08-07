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

import org.openrepose.commons.utils.io.stream.ReadLimitReachedException
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.routing.DestinationLocation
import org.openrepose.core.filter.routing.DestinationLocationBuilder
import org.openrepose.core.services.reporting.ReportingService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.systemmodel.config.Destination
import org.openrepose.core.systemmodel.config.DestinationEndpoint
import org.openrepose.core.systemmodel.config.ReposeCluster
import org.openrepose.nodeservice.request.RequestHeaderService
import org.openrepose.nodeservice.response.ResponseHeaderService
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.*

class PowerFilterRouterImplTest extends Specification {

    PowerFilterRouterImpl powerFilterRouter

    DestinationLocationBuilder destinationLocationBuilder = mock(DestinationLocationBuilder.class)
    ReposeCluster reposeCluster = mock(ReposeCluster.class)
    ServletContext servletContext = mock(ServletContext.class)
    RequestHeaderService requestHeaderService = mock(RequestHeaderService.class)
    ResponseHeaderService responseHeaderService = mock(ResponseHeaderService.class)
    ReportingService reportingService = mock(ReportingService.class)
    Optional<MetricsService> metricsService = Optional.empty()

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
            metricsService
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
    def "route default destination: #defaultDestinationId, dispatch error: #dispatchError"() {
        given:
        Map<String, Destination> destinationMap = new HashMap<>()
        Destination destinationOne = new DestinationEndpoint()
        destinationOne.setDefault("one" == defaultDestinationId)
        destinationOne.setId("one")
        destinationOne.setProtocol("http")
        destinationMap.put("one", destinationOne)
        Destination destinationTwo = new DestinationEndpoint()
        destinationTwo.setDefault("two" == defaultDestinationId)
        destinationTwo.setId("two")
        destinationTwo.setProtocol("http")
        destinationMap.put("two", destinationTwo)
        powerFilterRouter = new PowerFilterRouterImpl(destinationLocationBuilder,
            destinationMap,
            reposeCluster,
            defaultDestinationId,
            servletContext,
            requestHeaderService,
            responseHeaderService,
            reportingService,
            metricsService
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
                new URL(destinationOne.getProtocol(), "localhost", 8080, destinationOne.getRootPath()),
                new URI("http", null, "localhost", 8080, destinationOne.getRootPath(),"", null)
            )
        )

        if (dispatchError != null) {
            when(requestDispatcher.forward(any(ServletRequest.class), any(ServletResponse.class))).thenThrow(dispatchError)
        }

        when:
        powerFilterRouter.route(httpServletRequestWrapper, httpServletResponse)

        then:
        assert httpServletResponse.getStatus() == responseCode

        if (defaultDestinationId == null) {
            verify(requestDispatcher, never()).forward(
                any(ServletRequest.class), any(ServletResponse.class))
            verify(reportingService, never()).incrementRequestCount(anyString())
        } else {
            verify(requestDispatcher, times(1)).forward(
                any(ServletRequest.class), any(ServletResponse.class))
            verify(reportingService, times(1)).incrementRequestCount(anyString())
        }

        where:

        defaultDestinationId | responseCode                                    | dispatchError
        null                 | HttpServletResponse.SC_OK                       | null
        null                 | HttpServletResponse.SC_OK                       | new IOException()
        null                 | HttpServletResponse.SC_OK                       | new IOException().initCause(new ReadLimitReachedException("too much"))
        "one"                | HttpServletResponse.SC_OK                       | null
        "one"                | HttpServletResponse.SC_SERVICE_UNAVAILABLE      | new IOException()
        "one"                | HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE | new IOException().initCause(new ReadLimitReachedException("too much"))
        "two"                | HttpServletResponse.SC_OK                       | null
        "two"                | HttpServletResponse.SC_SERVICE_UNAVAILABLE      | new IOException()
        "two"                | HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE | new IOException().initCause(new ReadLimitReachedException("too much"))
    }
}
