package com.rackspace.papi.filter;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class PowerFilterTest {
    public static class WhenUsingPowerFilterTest {
        @Test
        public void shouldInstantiate() {
            PowerFilter powerFilter = new PowerFilter();

            assertNotNull(powerFilter);            
        }

        @Test
        public void shouldDestroy() {
            PowerFilter powerFilter = new PowerFilter();
            powerFilter.destroy();
        }

        @Test
        public void shouldDoFilter() throws IOException, ServletException {
            Enumeration<String> mockedHeaderNames = mock(Enumeration.class);
            FilterChain mockedFilterChain = mock(FilterChain.class);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeaderNames()).thenReturn(mockedHeaderNames);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getStatus()).thenReturn(200);

            PowerFilter powerFilter = new PowerFilter();

            powerFilter.doFilter(request, response, mockedFilterChain);
        }
    }
}
