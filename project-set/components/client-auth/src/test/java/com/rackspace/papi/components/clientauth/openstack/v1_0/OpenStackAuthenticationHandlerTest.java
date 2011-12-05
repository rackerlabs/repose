package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.openstack.config.OpenStackIdentityService;
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
        protected OpenstackAuth osauthConfig;

        @Before
        public void beforeAny() {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            osauthConfig = new OpenstackAuth();
            osauthConfig.setDelegatable(delegatable());

            final ClientMapping mapping = new ClientMapping();
            mapping.setIdRegex("/start/(.*)/");

            osauthConfig.getClientMapping().add(mapping);

            final OpenStackIdentityService openStackIdentityService = new OpenStackIdentityService();
            openStackIdentityService.setUri("http://some.auth.endpoint");
            osauthConfig.setIdentityService(openStackIdentityService);

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

    public static class WhenAuthenticatingDelegatableRequests extends TestParent {

        @Override
        protected boolean delegatable() {
            return true;
        }

        @Before
        public void standUp() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
        }

        @Test
        public void shouldPassNullOrBlankCredentials() {
            when(request.getRequestURI()).thenReturn("/start/");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must pass requests with invalid credentials", FilterAction.PROCESS_RESPONSE, requestDirector.getFilterAction());
        }

        @Test
        public void shouldRejectInvalidCredentials() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must reject requests with invalid credentials", FilterAction.RETURN, requestDirector.getFilterAction());
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

    public static class WhenHandlingResponseFromServiceInDelegatedMode extends TestParent {
        @Override
        protected boolean delegatable() {
            return true;
        }

        @Test
        public void shouldNotModifyResponseOnResponseStatusCodeNotEqualTo401or403() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            when(response.getStatus()).thenReturn(200);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must pass valid, delegated responses", FilterAction.NOT_SET, responseDirector.getFilterAction());
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn401() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            final String expected = "Keystone uri=" + osauthConfig.getIdentityService().getUri();

            assertEquals("Auth component must pass invalid requests but process their responses", expected, responseDirector.responseHeaderManager().headersToAdd().get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn403() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(403);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            final String expected = "Keystone uri=" + osauthConfig.getIdentityService().getUri();

            assertEquals("Auth component must pass invalid requests but process their responses", expected, responseDirector.responseHeaderManager().headersToAdd().get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
        }

        @Test
        public void shouldReturn500OnAuth501FailureWithDelegatedWwwAuthenticateHeaderSet() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }
    }

    public static class WhenHandlingResponseFromServiceNotInDelegatedMode extends TestParent {
        @Override
        protected boolean delegatable() {
            return false;
        }

        @Test
        public void shouldReturn501OnAuthFailureWithNonDelegatedWwwAuthenticateHeaderSet() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }

        @Test
        public void shouldReturn501OnAuthFailureWithNoWwwAuthenticateHeaderSet() {
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }

        @Test
        public void shouldReturn501OnAuth501FailureWithDelegatedWwwAuthenticateHeaderNotSet() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.NOT_IMPLEMENTED, responseDirector.getResponseStatus());
        }
    }

}
