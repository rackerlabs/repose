package org.openrepose.core.filter;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.context.container.ContainerConfigurationService;
import org.openrepose.powerfilter.PowerFilterChain;
import org.openrepose.powerfilter.PowerFilterChainException;
import org.openrepose.powerfilter.PowerFilterRouter;
import org.openrepose.powerfilter.filtercontext.FilterContext;
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
 * TODO: I don't think this test does anything, because it's got a lot of stuff in it that shouldn't be here...
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
            ContainerConfigurationService containerConfigurationService = mock(ContainerConfigurationService.class);
            when(containerConfigurationService.getVia()).thenReturn("");
            when(mockedFilterContext.getFilter()).thenReturn(mockedFilter);
            filterContextList.add(mockedFilterContext);
            FilterChain mockedFilterChain = mock(FilterChain.class);

            ServletContextHelper instance = ServletContextHelper.configureInstance(context, appContext);
            when(context.getAttribute(ServletContextHelper.SERVLET_CONTEXT_HELPER)).thenReturn(instance);

            PowerFilterChain powerFilterChainState = new PowerFilterChain(filterContextList, mockedFilterChain, mock(PowerFilterRouter.class), null);

            HttpServletRequest mockedServletRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockedServletResponse = mock(HttpServletResponse.class);

            when(mockedServletRequest.getRequestURL()).thenReturn(new StringBuffer());

            powerFilterChainState.startFilterChain(mockedServletRequest, mockedServletResponse);

            powerFilterChainState.startFilterChain(mockedServletRequest, mockedServletResponse);
        }
    }
}
