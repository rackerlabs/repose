package com.rackspace.papi.filter
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse
import com.rackspace.papi.domain.ReposeInstanceInfo
import com.rackspace.papi.filter.resource.ResourceMonitor
import com.rackspace.papi.service.reporting.metrics.MetricsService
import org.mockito.Mockito
import spock.lang.Ignore
import spock.lang.Specification

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.powermock.api.mockito.PowerMockito.when

class PowerFilterChainTest extends Specification {
    PowerFilterChain powerFilterChain
    MutableHttpServletRequest mutableHttpRequest
    MutableHttpServletResponse mutableHttpResponse
    HttpServletResponse servletResponse
    HttpServletRequest servletRequest
    FilterContext filterContext
    Filter filter
    com.rackspace.papi.model.Filter filterConfig
    ClassLoader classLoader
    List<FilterContext> filterChainCopy
    FilterChain containerFilterChain
    ResourceMonitor resourceMonitor
    PowerFilterRouter router
    ReposeInstanceInfo instanceInfo
    MetricsService metricsService

    //still failing
    @Ignore
    def "on doFilter, when an error is encountered it should return a 500 instead of a 200"() {
        given:
        mutableHttpRequest = mock(MutableHttpServletRequest)
        servletRequest = mock(HttpServletRequest)
        servletResponse = mock(HttpServletResponse)
        classLoader = mock(ClassLoader)
        filter = mock(Filter)
        filterConfig = mock(com.rackspace.papi.model.Filter)
        filterContext = new FilterContext(filter, classLoader, filterConfig)
        filterChainCopy = new LinkedList<FilterContext>();
        containerFilterChain = mock(FilterChain)
        resourceMonitor = mock(ResourceMonitor)
        router = mock(PowerFilterRouter)
        instanceInfo = mock(ReposeInstanceInfo)
        metricsService = mock(MetricsService)

        mutableHttpResponse = new MutableHttpServletResponse(servletRequest, servletResponse)

        powerFilterChain = new PowerFilterChain(filterChainCopy, containerFilterChain, resourceMonitor, router,
                instanceInfo, metricsService)

        when(servletResponse instanceof MutableHttpServletResponse).thenReturn(false)
        when(MutableHttpServletResponse.wrap(Mockito.any(MutableHttpServletRequest), Mockito.any(MutableHttpServletResponse))).thenReturn(mutableHttpResponse)

        doThrow(new IOException()).when(filter).doFilter(Mockito.any(MutableHttpServletRequest),
                Mockito.any(MutableHttpServletResponse), Mockito.any(PowerFilterChain))

        when:
        powerFilterChain.doReposeFilter(mutableHttpRequest, servletResponse, filterContext)

        then:
        mutableHttpResponse.getStatus() == 500
    }
}
