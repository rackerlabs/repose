package org.openrepose.core.servlet.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.systemmodel.DestinationCluster;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Vector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class FilterDirectorImplTest {

    public static class WhenCreatingNewInstances {

        @Test
        public void shouldProvideNonNullDefaults() {
            final FilterDirectorImpl impl = new FilterDirectorImpl();

            assertNotNull("By default, the delegated action should not be null", impl.getFilterAction());
            assertNotNull("By default, the delegated status should not be null", impl.getResponseStatusCode());
            assertNotNull("By default, the message body should not be null", impl.getResponseMessageBody());
            assertNotNull("By default, should have output stream", impl.getResponseOutputStream());
            assertNotNull("By default, should have print writer", impl.getResponseWriter());
        }
    }

    public static class WhenProcessingResponses {

        private FilterDirectorImpl impl;
        private MutableHttpServletResponse response;
        private HttpServletResponse httpResponse;
        private HttpServletRequest httpRequest;

        @Before
        public void setup() {
            impl = new FilterDirectorImpl();

            httpResponse = mock(HttpServletResponse.class);
            httpRequest = mock(HttpServletRequest.class);
            response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        }

        @Test
        public void shouldAddResponseHeader() throws IOException {
            impl.responseHeaderManager().appendHeader("key", "value");
            impl.applyTo(response);
            assertEquals("value", response.getHeader("key"));
        }

        @Test
        public void shouldSetStatusCode() throws IOException {
            final int status = 201;
            impl.setResponseStatusCode(status);
            impl.setFilterAction(FilterAction.PASS);
            impl.applyTo(response);
            verify(httpResponse).setStatus(status);
            assertEquals(FilterAction.PASS, impl.getFilterAction());
            assertEquals(new Integer(status), new Integer(impl.getResponseStatusCode()));
        }

        @Test
        public void shouldSetStatusCode2() throws IOException {
            final int status = 201;
            impl.setResponseStatusCode(status);
            impl.setFilterAction(FilterAction.PASS);
            impl.applyTo(response);
            verify(httpResponse).setStatus(status);
            assertEquals(status, impl.getResponseStatusCode());
            assertEquals(FilterAction.PASS, impl.getFilterAction());
        }
    }

    public static class WhenProcessingRequests {

        private FilterDirectorImpl impl;
        private MutableHttpServletRequest request;
        private HttpServletRequest httpRequest;

        @Before
        public void setup() {
            impl = new FilterDirectorImpl();

            httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getHeaderNames()).thenReturn(new Vector<String>().elements());

            request = MutableHttpServletRequest.wrap(httpRequest);
        }

        @Test
        public void shouldAddDestinations() {
            final float quality = new Float(1.5);
            final String uri = "uri";
            final String destId = "destId";

            impl.addDestination(destId, uri, quality);
            impl.applyTo(request);
            RouteDestination destination = request.getDestination();

            assertNotNull(destination);
            assertNotNull(impl.getDestinations());
            assertTrue(impl.getDestinations().size() > 0);
            assertEquals(destId, destination.getDestinationId());
            assertEquals(uri, destination.getUri());
            assertEquals(new Float(quality), new Float(destination.getQuality()));
        }

        @Test
        public void shouldAddDestinations2() {
            final float quality = new Float(1.5);
            final String uri = "uri";
            final String destId = "destId";

            DestinationCluster dest = new DestinationCluster();
            dest.setId(destId);

            impl.addDestination(dest, uri, quality);
            impl.applyTo(request);
            RouteDestination destination = request.getDestination();

            assertNotNull(destination);
            assertNotNull(impl.getDestinations());
            assertTrue(impl.getDestinations().size() > 0);
            assertEquals(destId, destination.getDestinationId());
            assertEquals(uri, destination.getUri());
            assertEquals(new Float(quality), new Float(destination.getQuality()));
        }

        @Test
        public void shouldSetRequestUri() {
            final String uri = "someuri";
            impl.setRequestUri(uri);
            impl.applyTo(request);

            request.getRequestURI();
            verify(httpRequest).setAttribute(eq("repose.request.uri"), eq(uri));
            verify(httpRequest).getAttribute(eq("repose.request.uri"));

            //assertEquals(uri, impl.getRequestUri());
            //assertEquals(uri, request.getRequestURI());
        }

        @Test
        public void shouldSetRequestUrl() {
            final String url = "http://somehost:8080/someuri";
            final StringBuffer urlBuffer = new StringBuffer(url);
            impl.setRequestUrl(urlBuffer);
            impl.applyTo(request);

            request.getRequestURL();
            verify(httpRequest).setAttribute(eq("repose.request.url"), eq(urlBuffer));
            verify(httpRequest).getAttribute(eq("repose.request.url"));
            //assertEquals(url, impl.getRequestUrl().toString());
            //assertEquals(url, request.getRequestURL().toString());
        }

        @Test
        public void shouldSetRequestUriQuery() {
            final String queryString = "a=1&b=2&c=3";
            impl.setRequestUriQuery(queryString);
            impl.applyTo(request);
            request.getQueryString();
            verify(httpRequest).setAttribute(eq("repose.request.querystring"), eq(queryString));
            verify(httpRequest).getAttribute(eq("repose.request.querystring"));
            //assertEquals("When a URI query string is set, it must be rewritten it in the request object during applicaiton.", queryString, request.getQueryString());
        }

        @Test
        public void shouldAddRequestHeader() {
            impl.requestHeaderManager().appendHeader("key", "value");
            impl.applyTo(request);
            assertEquals("value", request.getHeader("key"));
        }
    }
}
