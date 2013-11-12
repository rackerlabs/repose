package com.rackspace.papi.filter;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.context.impl.RoutingServiceContext;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class RequestFilterChainStateTest {

    public static class WhenUsingPowerFilterChain {

        @Test
        public void shouldDoFilter() throws IOException, ServletException, NamingException, PowerFilterChainException {
            List<FilterContext> filterContextList = new ArrayList<FilterContext>();
            Filter mockedFilter = mock(Filter.class);
            FilterContext mockedFilterContext = mock(FilterContext.class);
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            ServletContext context = mock(ServletContext.class);
            ApplicationContext appContext = mock(ApplicationContext.class);
            RoutingServiceContext routingContext = mock(RoutingServiceContext.class);
            ContainerConfigurationService containerConfigurationService = mock(ContainerConfigurationService.class);
            when(containerConfigurationService.getVia()).thenReturn("");
            when(mockedFilterContext.getFilter()).thenReturn(mockedFilter);
            when(mockedFilterContext.getFilterClassLoader()).thenReturn(mockedClassLoader);
            when(appContext.getBean(anyString())).thenReturn(routingContext);
            filterContextList.add(mockedFilterContext);
            FilterChain mockedFilterChain = mock(FilterChain.class);

            ServletContextHelper instance = ServletContextHelper.configureInstance(context, appContext);
            when(context.getAttribute(ServletContextHelper.SERVLET_CONTEXT_HELPER)).thenReturn(instance);

            ReposeInstanceInfo instanceInfo = new ReposeInstanceInfo("repose", "node");
            PowerFilterChain powerFilterChainState = new PowerFilterChain(filterContextList, mockedFilterChain, mock(ResourceMonitor.class), mock(PowerFilterRouter.class), instanceInfo, null);

            HttpServletRequest mockedServletRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockedServletResponse = mock(HttpServletResponse.class);

            when(mockedServletRequest.getRequestURL()).thenReturn(new StringBuffer());

            powerFilterChainState.startFilterChain(mockedServletRequest, mockedServletResponse);

            powerFilterChainState.startFilterChain(mockedServletRequest, mockedServletResponse);
        }
    }
}
