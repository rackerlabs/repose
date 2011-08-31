package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import org.junit.Ignore;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RateLimitingHandlerTest extends RateLimitingTestSupport {

    public static class WhenMakingInvalidRequests extends DefaultTestCase {

        @Test
        public void shouldReturnUnauthorizedWhenUserInformationIsMissing() {
            final FilterDirector director = handler.handleRequest(requestMock);

            assertEquals("FilterDirectory must return on rate limiting failure", FilterAction.RETURN, director.getFilterAction());
            assertEquals("Must return 401 if the user has not been identified", HttpStatusCode.UNAUTHORIZED, director.getResponseStatus());
        }
    }

    public static class WhenMakingValidRequests extends DefaultTestCase {

        @Test
        public void shouldPassValidRequests() {
            final Enumeration<String> groupHeaders = mock(Enumeration.class);
            when(groupHeaders.hasMoreElements()).thenReturn(Boolean.FALSE);

            when(requestMock.getHeader(PowerApiHeader.USER.headerKey())).thenReturn("user");
            when(requestMock.getHeader(PowerApiHeader.GROUPS.headerKey())).thenReturn("group");
            when(requestMock.getHeaders(PowerApiHeader.GROUPS.headerKey())).thenReturn(groupHeaders);
            when(requestMock.getMethod()).thenReturn("GET");
            when(requestMock.getRequestURI()).thenReturn("/v1.0/12345/resource");
            when(requestMock.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/12345/resource"));

            final FilterDirector director = handler.handleRequest(requestMock);

            assertEquals("Filter must pass valid, non-limited requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldProcessResponseWhenAbsoluteLimitsIntegrationIsEnabled() {
            final Enumeration<String> groupHeaders = mock(Enumeration.class);
            when(groupHeaders.hasMoreElements()).thenReturn(Boolean.FALSE);

            when(requestMock.getHeader(PowerApiHeader.USER.headerKey())).thenReturn("user");
            when(requestMock.getHeader(PowerApiHeader.GROUPS.headerKey())).thenReturn("group");
            when(requestMock.getHeaders(PowerApiHeader.GROUPS.headerKey())).thenReturn(groupHeaders);
            when(requestMock.getMethod()).thenReturn("GET");
            when(requestMock.getRequestURI()).thenReturn("/v1.0/limits");
            when(requestMock.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));

            final FilterDirector director = handler.handleRequest(requestMock);

            assertEquals("On successful pass, filter must process response", FilterAction.PROCESS_RESPONSE, director.getFilterAction());
        }
    }

    @Ignore
    public static class DefaultTestCase {

        protected RateLimitingHandler handler;
        protected HttpServletRequest requestMock;

        @Before
        public void standUp() {
            final Datastore datastoreMock = mock(Datastore.class);
            when(datastoreMock.get(anyString())).thenReturn(new StoredElementImpl("key", null));

            handler = new RateLimitingHandler(datastoreMock);
            handler.getRateLimitingConfigurationListener().configurationUpdated(defaultRateLimitingConfiguration());

            requestMock = mock(HttpServletRequest.class);
        }
    }
}
