package com.rackspace.papi.filter
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse
import com.rackspace.papi.domain.ReposeInstanceInfo
import com.rackspace.papi.filter.resource.ResourceMonitor
import com.rackspace.papi.model.Filter
import com.rackspace.papi.service.reporting.metrics.MetricsService
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.mock

class PowerFilterChainTest extends Specification {
    PowerFilterChain powerFilterChain
    MutableHttpServletRequest mutableHttpRequest
    MutableHttpServletResponse mutableHttpResponse
    HttpServletResponse servletResponse
    FilterContext filterContext
    ClassLoader classLoader
    List<FilterContext> filterChainCopy
    FilterChain containerFilterChain
    ResourceMonitor resourceMonitor
    PowerFilterRouter router
    ReposeInstanceInfo instanceInfo
    MetricsService metricsService


    //currently failing
    def "on doFilter, when an error is encountered it should return a 500 instead of a 200"() {
        given:
        mutableHttpResponse = mock(MutableHttpServletResponse)
        mutableHttpRequest = mock(MutableHttpServletRequest)
        servletResponse = mock(HttpServletResponse)
        classLoader = mock(ClassLoader)

        Filter filter = new Filter()
        filter.setName("foolter")
        filterContext = new FilterContext(null, null, filter) //getting this to not npe is a *****

        filterChainCopy = new LinkedList<FilterContext>();
        containerFilterChain = mock(FilterChain)
        resourceMonitor = mock(ResourceMonitor)
        router = mock(PowerFilterRouter)
        instanceInfo = mock(ReposeInstanceInfo)
        metricsService = mock(MetricsService)

        powerFilterChain = new PowerFilterChain(filterChainCopy, containerFilterChain, resourceMonitor, router, instanceInfo, metricsService)

        MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse) >> mutableHttpResponse
        filterContext.getFilterClassLoader() >> classLoader
        mutableHttpResponse.pushOutputStream() >> void

        when:
        powerFilterChain.doReposeFilter(mutableHttpRequest, servletResponse, filterContext)

        then:
        mutableHttpResponse.getStatus() == 500
    }
}
