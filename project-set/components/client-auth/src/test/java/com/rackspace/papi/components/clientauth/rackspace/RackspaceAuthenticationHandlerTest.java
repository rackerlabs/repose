package com.rackspace.papi.components.clientauth.rackspace;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AuthenticationServer;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandler;
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
        // TODO: Refactor Rackspace Auth Service to include and interface that can be
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

            rackAuthConfig.getAccountMapping().add(mapping);

            final AuthenticationServer authenticationServer = new AuthenticationServer();
            authenticationServer.setUri("http://some.auth.endpoint");
            rackAuthConfig.setAuthenticationServer(authenticationServer);

            handler = new RackspaceAuthenticationHandler(rackAuthConfig);
        }

        protected abstract boolean delegatable();
    }

    public static class WhenAuthenticatingNonDelegatableRequests extends TestParent {
        @Override
        protected boolean delegatable() {
            return false;
        }

        @Before
        public void standUp() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
        }

        @Test
        @Ignore
        public void shouldNotReturnNullFilterDirectorOnResponseHandling() {
            final FilterDirector director = handler.handleResponse(request, response);

            assertNotNull("FilterDirector should not be null", director);
        }
    }

    public static class WhenGettingWWWAuthenticateHeaderContents extends TestParent {

        @Override
        protected boolean delegatable() {
            return false;
        }

        @Before
        public void setup() {
        }

        @Test
        public void shouldReturnRackspaceWWWAuthenticateHeaderContents() {
            String expected = "RackAuth Realm=\"API Realm\"";

            String actual = super.handler.getWWWAuthenticateHeaderContents();

            assertEquals(expected, actual);
        }
    }
}
