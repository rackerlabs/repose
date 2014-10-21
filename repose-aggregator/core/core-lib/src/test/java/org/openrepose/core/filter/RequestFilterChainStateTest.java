package org.openrepose.core.filter;

import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.context.container.ContainerConfigurationService;
import org.openrepose.core.services.context.impl.RoutingServiceContext;
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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            ServletContext context = mock(ServletContext.class);
            ApplicationContext appContext = mock(ApplicationContext.class);
            RoutingServiceContext routingContext = mock(RoutingServiceContext.class);
            ContainerConfigurationService containerConfigurationService = mock(ContainerConfigurationService.class);
            when(containerConfigurationService.getVia()).thenReturn("");
            when(mockedFilterContext.getFilter()).thenReturn(mockedFilter);
            when(appContext.getBean(anyString())).thenReturn(routingContext);
            filterContextList.add(mockedFilterContext);
            FilterChain mockedFilterChain = mock(FilterChain.class);

            ServletContextHelper instance = ServletContextHelper.configureInstance(context, appContext);
            when(context.getAttribute(ServletContextHelper.SERVLET_CONTEXT_HELPER)).thenReturn(instance);

            ReposeInstanceInfo instanceInfo = new ReposeInstanceInfo("repose", "node");
            PowerFilterChain powerFilterChainState = new PowerFilterChain(filterContextList, mockedFilterChain, mock(PowerFilterRouter.class), instanceInfo, null);

            HttpServletRequest mockedServletRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockedServletResponse = mock(HttpServletResponse.class);

            when(mockedServletRequest.getRequestURL()).thenReturn(new StringBuffer());

            powerFilterChainState.startFilterChain(mockedServletRequest, mockedServletResponse);

            powerFilterChainState.startFilterChain(mockedServletRequest, mockedServletResponse);
        }
    }
}
