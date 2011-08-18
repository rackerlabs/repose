package com.rackspace.papi.filter;

import javax.servlet.*;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
        public void shouldDoFilter() throws IOException, ServletException {
            List<FilterContext> filterContextList = new ArrayList<FilterContext>();
            Filter mockedFilter = mock(Filter.class);
            FilterContext mockedFilterContext = mock(FilterContext.class);
            ClassLoader mockedClassLoader = mock(ClassLoader.class);
            when(mockedFilterContext.getFilter()).thenReturn(mockedFilter);
            when(mockedFilterContext.getFilterClassLoader()).thenReturn(mockedClassLoader);
            filterContextList.add(mockedFilterContext);
            FilterChain mockedFilterChain = mock(FilterChain.class);

            RequestFilterChainState powerFilterChainState = new RequestFilterChainState(filterContextList, mockedFilterChain);

            ServletRequest mockedServletRequest = mock(ServletRequest.class);
            ServletResponse mockedServletResponse = mock(ServletResponse.class);

            powerFilterChainState.doFilter(mockedServletRequest, mockedServletResponse);

            powerFilterChainState.doFilter(mockedServletRequest, mockedServletResponse);
        }

        
    }
}
