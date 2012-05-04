package com.rackspace.papi.filter;

import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.ServiceDomain;
import com.rackspace.papi.service.context.impl.RoutingServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.jndi.JndiContextAdapterProvider;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingException;

import javax.servlet.http.HttpServletRequest;
import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class RequestFilterChainStateTest {

    public static class WhenUsingPowerFilterChain {

        @Test
        public void shouldDoFilter() throws IOException, ServletException, NamingException {
            List<FilterContext> filterContextList = new ArrayList<FilterContext>();
            Filter mockedFilter = mock(Filter.class);
            FilterContext mockedFilterContext = mock(FilterContext.class);
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            ServletContext context = mock(ServletContext.class);
            Context namingContext = mock(Context.class);
            RoutingServiceContext routingContext = mock(RoutingServiceContext.class);
            when(mockedFilterContext.getFilter()).thenReturn(mockedFilter);
            when(mockedFilterContext.getFilterClassLoader()).thenReturn(mockedClassLoader);
            when(context.getAttribute(ServletContextHelper.SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(namingContext);
            when(namingContext.lookup(RoutingServiceContext.SERVICE_NAME)).thenReturn(routingContext);
            filterContextList.add(mockedFilterContext);
            FilterChain mockedFilterChain = mock(FilterChain.class);

            ServletContextHelper.configureInstance(new JndiContextAdapterProvider(), context, namingContext);
            
            PowerFilterChain powerFilterChainState = new PowerFilterChain(mock(ServiceDomain.class), mock(DomainNode.class), filterContextList, mockedFilterChain, context, mock(ResourceMonitor.class));

            HttpServletRequest mockedServletRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockedServletResponse = mock(HttpServletResponse.class);

            when(mockedServletRequest.getRequestURL()).thenReturn(new StringBuffer());

            powerFilterChainState.doFilter(mockedServletRequest, mockedServletResponse);

            powerFilterChainState.doFilter(mockedServletRequest, mockedServletResponse);
        }
    }
}
