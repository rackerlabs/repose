package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class OpenStackAuthenticationHandlerTest {

    @Ignore
    public static abstract class TestParent {

        protected HttpServletRequest request;
        protected ReadableHttpServletResponse response;
        protected OpenStackAuthenticationService authService;
        protected OpenStackAuthenticationHandler handler;

        @Before
        public void beforeAny() {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            final OpenstackAuth osauthConfig = new OpenstackAuth();
            osauthConfig.setDelegatable(delegatable());

            final ClientMapping mapping = new ClientMapping();
            mapping.setIdRegex("/start/(.*)/");

            osauthConfig.getClientMapping().add(mapping);

            authService = mock(OpenStackAuthenticationService.class);
            handler = new OpenStackAuthenticationHandler(osauthConfig, authService);
        }

        protected abstract boolean delegatable();

        public CachableTokenInfo generateCachableTokenInfo(String roles, String tokenId, String username) {
            final CachableTokenInfo cti = mock(CachableTokenInfo.class);
            when(cti.getRoles()).thenReturn(roles);
            when(cti.getTokenId()).thenReturn(tokenId);
            when(cti.getUsername()).thenReturn(username);

            return cti;
        }
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
        public void shouldPassValidCredentials() {
            final CachableTokenInfo tokenInfo = generateCachableTokenInfo("", "", "");
            when(authService.validateToken(anyString(), anyString())).thenReturn(tokenInfo);

            final FilterDirector director = handler.handleRequest(request, response);

            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldRejectInvalidCredentials() {
            final FilterDirector director = handler.handleRequest(request, response);

            assertEquals("Auth component must reject invalid requests", FilterAction.RETURN, director.getFilterAction());
        }
    }

    public static class WhenAuthenticatingDelegatableRequests extends TestParent {

        @Override
        protected boolean delegatable() {
            return true;
        }

        @Before
        public void standUp() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
        }

        @Test
        public void shouldPassInvalidCredentials() {
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must pass invalid requests but process their responses", FilterAction.PROCESS_RESPONSE, requestDirector.getFilterAction());
        }

        @Test @Ignore
        public void shouldModifyWwwAuthenticateHeaderOn401() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(Integer.valueOf(401));

            final FilterDirector responseDirector = handler.handleResponse(request, response);
            final Map<String, Set<String>> headers = responseDirector.responseHeaderManager().headersToAdd();

            final Set<String> headerValues = headers.get(CommonHttpHeader.WWW_AUTHENTICATE.headerKey());

            assertEquals("Auth component must pass invalid requests but process their responses", "TODO", headerValues.iterator().next());
        }
    }
}
