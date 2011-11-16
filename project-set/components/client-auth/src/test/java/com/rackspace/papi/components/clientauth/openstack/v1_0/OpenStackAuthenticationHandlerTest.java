package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class OpenStackAuthenticationHandlerTest {

    @Ignore
    public static class TestParent {

        protected HttpServletRequest request;
        protected ReadableHttpServletResponse response;
        protected OpenStackAuthenticationService authService;
        protected OpenStackAuthenticationHandler handler;

        @Before
        public void beforeAny() {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            final OpenstackAuth osauthConfig = new OpenstackAuth();
            osauthConfig.setDelegatable(false);

            final ClientMapping mapping = new ClientMapping();
            mapping.setIdRegex("/start/(.*)/");

            osauthConfig.getClientMapping().add(mapping);

            authService = mock(OpenStackAuthenticationService.class);
            handler = new OpenStackAuthenticationHandler(osauthConfig, authService);
        }
    }

    public static class WhenAuthenticatingRequests extends TestParent {

        @Before
        public void standUp() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");

            final CachableTokenInfo cti = mock(CachableTokenInfo.class);
            when(cti.getRoles()).thenReturn("");
            when(cti.getTokenId()).thenReturn("");
            when(cti.getUsername()).thenReturn("");

            when(authService.validateToken(anyString(), anyString())).thenReturn(cti);
        }

        @Test
        public void shouldPassValidCredentials() {
            final FilterDirector director = handler.handleRequest(request, response);

            assertEquals("Auth component must pass valid requests", director.getFilterAction(), FilterAction.PASS);
        }
    }

    @Ignore
    public static class WhenHandlingDelegatedAuthenticationResponses {
    }
}
