package com.rackspace.papi.components.clientauth.rackspace;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountType;
import com.rackspace.papi.components.clientauth.rackspace.config.AuthenticationServer;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class RackspaceAuthenticationHandlerTest {

    @Ignore
    public static abstract class TestParent {

        protected HttpServletRequest request;
        protected ReadableHttpServletResponse response;
        // TODO: Refactor Rackspace Auth Service to include an interface that can be
        // injected into the Handler to make testing easier.  See OpenStackAuthenticationService below for example.
//        protected OpenStackAuthenticationService authService;
        protected RackspaceAuthenticationHandler handler;
        protected RackspaceAuth rackAuthConfig;

        @Before
        public void beforeAny() {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            rackAuthConfig = new RackspaceAuth();
            rackAuthConfig.setDelegatable(delegatable());

            final AccountMapping mapping = new AccountMapping();
            mapping.setIdRegex("/start/(.*)/");
            mapping.setType(AccountType.CLOUD);

            rackAuthConfig.getAccountMapping().add(mapping);

            final AuthenticationServer authenticationServer = new AuthenticationServer();
            authenticationServer.setUri("http://some.auth.endpoint");
            rackAuthConfig.setAuthenticationServer(authenticationServer);

            handler = new RackspaceAuthenticationHandler(rackAuthConfig);
        }

        protected abstract boolean delegatable();
    }

    public static class WhenAuthenticatingDelegatableRequests extends TestParent {

        @Override
        protected boolean delegatable() {
            return true;
        }

        @Test
        public void shouldPassNullOrBlankCredentials() {            
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("Auth component must set X Authorization header when in delegated mode", requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next().equalsIgnoreCase("Proxy"));
            assertTrue("Auth component must set X Identity Status to Indeterminate when in delegated mode", requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next().equalsIgnoreCase("Indeterminate"));
            assertEquals("Auth component must pass requests with null or blank credentials when in delegated mode", FilterAction.PASS, requestDirector.getFilterAction());
        }

        @Test
        public void shouldPassNullOrBlankAuthToken() {
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("Auth component must set X Authorization header when in delegated mode", requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next().equalsIgnoreCase("Proxy accountId"));
            assertTrue("Auth component must set X Identity Status to Indeterminate when in delegated mode", requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next().equalsIgnoreCase("Indeterminate"));
            assertEquals("Auth component must pass requests with null or blank auth token when in delegated mode", FilterAction.PASS, requestDirector.getFilterAction());
        }

        @Test
        public void shouldPassNullOrBlankAccountId() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("Auth component must set X Authorization header when in delegated mode", requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next().equalsIgnoreCase("Proxy"));
            assertTrue("Auth component must set X Identity Status to Indeterminate when in delegated mode", requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next().equalsIgnoreCase("Indeterminate"));
            assertEquals("Auth component must pass requests with null or blank account id when in delegated mode", FilterAction.PASS, requestDirector.getFilterAction());
        }

        @Test
        public void shouldRejectInvalidCredentials() {
            // TODO: Write this test once handler has been refactored to take rackspace auth service
        }

        @Test
        public void shouldPassValidCredentials() {
            // TODO: Write this test once handler has been refactored to take rackspace auth service
        }
        
    }

    public static class WhenAuthenticatingNonDelegatableRequests extends TestParent {
        @Override
        protected boolean delegatable() {
            return false;
        }

        @Test
        @Ignore
        public void shouldNotReturnNullFilterDirectorOnResponseHandling() {
            final FilterDirector director = handler.handleResponse(request, response);

            assertNotNull("FilterDirector should not be null", director);
        }

        @Test
        public void shouldRejectNullOrBlankCredentials() {
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject requests with null or blank credentials when not in delegated mode", HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectNullOrBlankAuthToken() {
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject requests with null or blank auth token when not in delegated mode", HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectNullOrBlankAccountId() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject requests with null or blank account id when not in delegated mode", HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectInvalidCredentials() {
            // TODO: Write this test once handler has been refactored to take rackspace auth service
        }

        @Test
        public void shouldPassValidCredentials() {
            // TODO: Write this test once handler has been refactored to take rackspace auth service
        }
    }

    @Ignore //TODO: Ignore this until we fix the null filterDirector bug in the brown bag
    public static class WhenHandlingResponseFromServiceInDelegatedMode extends TestParent {
        @Override
        protected boolean delegatable() {
            return true;
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn401() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);
            final String expected = "RackAuth Realm=\"API Realm\"";

            assertEquals("Auth component must return a 401 when delegatable origin service returns a 401", HttpStatusCode.UNAUTHORIZED, responseDirector.getResponseStatus());
            assertEquals("Auth component must modify WWW-Authenticate header when delegatable origin service returns a 401", expected, responseDirector.responseHeaderManager().headersToAdd().get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn403() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(403);

            final FilterDirector responseDirector = handler.handleResponse(request, response);
            final String expected = "RackAuth Realm=\"API Realm\"";

            assertEquals("Auth component must return a 403 when delegatable origin service returns a 403", HttpStatusCode.FORBIDDEN, responseDirector.getResponseStatus());
            assertEquals("Auth component must modify WWW-Authenticate header when delegatable origin service returns a 403", expected, responseDirector.responseHeaderManager().headersToAdd().get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
        }

        @Test
        public void shouldReturn500OnAuthFailureWith501() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 500 when delegatable origin service returns a 501", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }
    }

    @Ignore //TODO: Ignore this until we fix the null filterDirector bug in the brown bag
    public static class WhenHandlingResponseFromServiceNotInDelegatedMode extends TestParent {
        @Override
        protected boolean delegatable() {
            return false;
        }

        @Test
        public void shouldReturn500OnAuthFailureWith401() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 500 when un-delegatable origin service returns a 401", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }

        @Test
        public void shouldReturn500OnAuthFailureWith403() {
            when(response.getStatus()).thenReturn(403);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 500 when un-delegatable origin service returns a 403", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }

        @Test
        public void shouldReturn501OnAuthFailureWith501() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 501 when un-delegatable origin service returns a 501", HttpStatusCode.NOT_IMPLEMENTED, responseDirector.getResponseStatus());
        }
    }
}
