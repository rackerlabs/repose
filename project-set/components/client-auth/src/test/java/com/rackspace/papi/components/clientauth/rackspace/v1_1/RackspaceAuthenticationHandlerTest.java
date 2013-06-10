package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.rackspace.AuthenticationService;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.IdentityStatus;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.common.Configurables;
import com.rackspace.papi.components.clientauth.common.EndpointsConfiguration;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountType;
import com.rackspace.papi.components.clientauth.rackspace.config.AuthenticationServer;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class RackspaceAuthenticationHandlerTest {

    @Ignore
    public static abstract class TestParent {
        protected static final long AUTH_GROUP_CACHE_TTL = 600000;
        protected static final long AUTH_TOKEN_CACHE_TTL = 5000;
        protected static final long AUTH_USER_CACHE_TTL = 5000;

        protected static String xAuthProxy = "Proxy";
        protected HttpServletRequest request;
        protected ReadableHttpServletResponse response;
        protected AuthenticationService authServiceClient;
        protected RackspaceAuthenticationHandler handler;
        protected RackspaceAuth rackAuthConfig;
        protected List<AuthGroup> groups;
        protected AuthGroups authGroups;
        protected KeyedRegexExtractor keyedRegexExtractor;
        protected List<Pattern> whiteListRegexPatterns;
        protected EndpointsConfiguration endpointsConfiguration;

        @Before
        public void beforeAny() {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            // Add a default group to the groups list
            groups = new ArrayList<AuthGroup>();

            final AuthGroup defaultGroup = mock(AuthGroup.class);//new AuthGroup();
            when(defaultGroup.getDescription()).thenReturn("A default group");
            when(defaultGroup.getId()).thenReturn("group-id");

            groups.add(defaultGroup);
            authGroups = new AuthGroups(groups);

            // Setup config
            rackAuthConfig = new RackspaceAuth();
            rackAuthConfig.setDelegable(delegable());

            keyedRegexExtractor = new KeyedRegexExtractor();

            final AccountMapping mapping = new AccountMapping();
            mapping.setIdRegex("/start/(.*)/");
            mapping.setType(AccountType.CLOUD);

            final AccountMapping mapping2 = new AccountMapping();
            mapping2.setIdRegex(".*\\?.*username=(.+)");
            mapping2.setType(AccountType.MOSSO);

            rackAuthConfig.getAccountMapping().add(mapping);
            rackAuthConfig.getAccountMapping().add(mapping2);

            keyedRegexExtractor.addPattern(mapping.getIdRegex(), mapping.getType());
            keyedRegexExtractor.addPattern(mapping2.getIdRegex(), mapping2.getType());

            final AuthenticationServer authenticationServer = new AuthenticationServer();
            authenticationServer.setUri("http://some.auth.endpoint");
            rackAuthConfig.setAuthenticationServer(authenticationServer);

            authServiceClient = mock(AuthenticationService.class);

            whiteListRegexPatterns = new ArrayList<Pattern>();
            whiteListRegexPatterns.add(Pattern.compile("/v1.0/application\\.wadl"));

            endpointsConfiguration = new EndpointsConfiguration(null, null, null);
            Configurables configurables = new Configurables(delegable(), "http://some.auth.endpoint", keyedRegexExtractor, true, AUTH_GROUP_CACHE_TTL, AUTH_TOKEN_CACHE_TTL,
                    AUTH_USER_CACHE_TTL, requestGroups(), endpointsConfiguration);
            handler = new RackspaceAuthenticationHandler(configurables, authServiceClient, null, null,null,null, new UriMatcher(whiteListRegexPatterns));
            endpointsConfiguration = new EndpointsConfiguration(null, null, null);
            
            handler = new RackspaceAuthenticationHandler(configurables, authServiceClient, null, null, null, null,
                                                         new UriMatcher(whiteListRegexPatterns));
        }

        protected abstract boolean delegable();

        protected abstract boolean requestGroups();

    }

    public static class WhenAuthenticatingDelegableRequests extends TestParent {

        @Override
        protected boolean delegable() {
            return true;
        }

        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Test
        public void shouldPassNullOrBlankCredentials() {
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("Auth component must set X Authorization header when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next()
                               .equalsIgnoreCase(xAuthProxy));
            assertTrue("Auth component must set X Identity Status to Indeterminate when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next()
                               .equalsIgnoreCase(IdentityStatus.Indeterminate.name()));
            assertEquals("Auth component must pass requests with null or blank credentials when in delegated mode",
                         FilterAction.PASS, requestDirector.getFilterAction());
        }

        @Test
        public void shouldPassNullOrBlankAuthToken() {
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("Auth component must set X Authorization header when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next()
                               .equalsIgnoreCase(xAuthProxy + " accountId"));
            assertTrue("Auth component must set X Identity Status to Indeterminate when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next()
                               .equalsIgnoreCase(IdentityStatus.Indeterminate.name()));
            assertEquals("Auth component must pass requests with null or blank auth token when in delegated mode",
                         FilterAction.PASS, requestDirector.getFilterAction());
        }

        @Test
        public void shouldPassNullOrBlankAccountId() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("Auth component must set X Authorization header when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next()
                               .equalsIgnoreCase(xAuthProxy));
            assertTrue("Auth component must set X Identity Status to Indeterminate when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next()
                               .equalsIgnoreCase(IdentityStatus.Indeterminate.name()));
            assertEquals("Auth component must pass requests with null or blank account id when in delegated mode",
                         FilterAction.PASS, requestDirector.getFilterAction());
        }

        @Test
        public void shouldRejectInvalidCredentials() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");
            when(authServiceClient.validateToken(any(ExtractorResult.class), anyString())).thenReturn(null);

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject invalid credentials with a 401 when in delegated mode",
                         HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldPassValidCredentials() {
            String tokenId = "some-random-auth-token";
            String userName = "userName";
            FullToken fullToken = new FullToken();
            fullToken.setId(tokenId);
            fullToken.setUserId(userName);
            AuthToken token = mock(AuthToken.class);
            when(request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");
            when(authServiceClient.validateToken(any(ExtractorResult.class), anyString())).thenReturn(token);
            when(authServiceClient.getGroups(anyString())).thenReturn(authGroups);

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("When groups exist, handler must add X-PP-Groups header",
                       requestDirector.requestHeaderManager().headersToAdd()
                               .get(PowerApiHeader.GROUPS.toString().toLowerCase()).contains("group-id;q=1"));
            assertTrue("Handler must add X-PP-User header", requestDirector.requestHeaderManager().headersToAdd()
                    .get(PowerApiHeader.USER.toString().toLowerCase()).contains("accountId;q=1"));
            assertTrue("Auth component must set X Authorization header when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next()
                               .equalsIgnoreCase(xAuthProxy + " accountId"));
            assertTrue("Auth component must set X Identity Status to Confirmed when in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-identity-status").iterator().next()
                               .equalsIgnoreCase(IdentityStatus.Confirmed.name()));
            assertEquals("Auth component must pass valid credentials when in delegated mode", FilterAction.PASS,
                         requestDirector.getFilterAction());
        }
    }

    public static class WhenAuthenticatingNonDelegableRequests extends TestParent {

        @Override
        protected boolean delegable() {
            return false;
        }

        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Test
        public void shouldNotReturnNullFilterDirectorOnResponseHandling() {
            final FilterDirector director = handler.handleResponse(request, response);

            assertNotNull("FilterDirector should not be null", director);
        }

        @Test
        public void shouldHandleAuthenticationServiceFailures() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");
            when(authServiceClient.validateToken(any(ExtractorResult.class), anyString()))
                    .thenThrow(new RuntimeException());

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("When auth service fails, repose should return a 500", HttpStatusCode.INTERNAL_SERVER_ERROR,
                         requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectNullOrBlankCredentials() {
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals(
                    "Auth component must reject requests with null or blank credentials when not in delegated mode",
                    HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectNullOrBlankAuthToken() {
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject requests with null or blank auth token when not in delegated mode",
                         HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectNullOrBlankAccountId() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/");

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject requests with null or blank account id when not in delegated mode",
                         HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldRejectInvalidCredentials() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");
            when(authServiceClient.validateToken(any(ExtractorResult.class), anyString())).thenReturn(null);

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertEquals("Auth component must reject invalid credentials with a 401 when in not in delegated mode",
                         HttpStatusCode.UNAUTHORIZED, requestDirector.getResponseStatus());
        }

        @Test
        public void shouldPassValidCredentials() {
            String tokenId = "some-random-auth-token";
            String userName = "userName";
            FullToken fullToken = new FullToken();
            fullToken.setId(tokenId);
            fullToken.setUserId(userName);
            AuthToken token = mock(AuthToken.class);
            when(request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(tokenId);
            when(request.getRequestURI()).thenReturn("/start/accountId/resource");
            when(authServiceClient.validateToken(any(ExtractorResult.class), anyString())).thenReturn(token);
            when(authServiceClient.getGroups(anyString())).thenReturn(authGroups);

            final FilterDirector requestDirector = handler.handleRequest(request, response);

            assertTrue("When groups exist, handler must add X-PP-Groups header",
                       requestDirector.requestHeaderManager().headersToAdd()
                               .get(PowerApiHeader.GROUPS.toString().toLowerCase()).contains("group-id;q=1"));
            assertTrue("Handler must add X-PP-User header", requestDirector.requestHeaderManager().headersToAdd()
                    .get(PowerApiHeader.USER.toString().toLowerCase()).contains("accountId;q=1"));
            assertTrue("Auth component must set X Authorization header when not in delegated mode",
                       requestDirector.requestHeaderManager().headersToAdd().get("x-authorization").iterator().next()
                               .equalsIgnoreCase(xAuthProxy + " accountId"));
            assertEquals("Auth component must pass valid credentials when not in delegated mode", FilterAction.PASS,
                         requestDirector.getFilterAction());
        }
    }

    public static class WhenHandlingResponseFromServiceInDelegatedMode extends TestParent {

        @Override
        protected boolean delegable() {
            return true;
        }

        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn401() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);
            final String expected = "RackAuth Realm=\"API Realm\"";

            assertEquals("Auth component must return a 401 when Delegable origin service returns a 401",
                         HttpStatusCode.UNAUTHORIZED, responseDirector.getResponseStatus());
            assertEquals(
                    "Auth component must modify WWW-Authenticate header when Delegable origin service returns a 401",
                    expected, responseDirector.responseHeaderManager().headersToAdd()
                    .get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn403() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(403);

            final FilterDirector responseDirector = handler.handleResponse(request, response);
            final String expected = "RackAuth Realm=\"API Realm\"";

            assertEquals("Auth component must return a 403 when Delegable origin service returns a 403",
                         HttpStatusCode.FORBIDDEN, responseDirector.getResponseStatus());
            assertEquals(
                    "Auth component must modify WWW-Authenticate header when Delegable origin service returns a 403",
                    expected, responseDirector.responseHeaderManager().headersToAdd()
                    .get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
        }

        @Test
        public void shouldReturn500OnAuthFailureWith501() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 500 when Delegable origin service returns a 501",
                         HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }
    }

    public static class WhenHandlingResponseFromServiceNotInDelegatedMode extends TestParent {

        @Override
        protected boolean delegable() {
            return false;
        }

        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Test
        public void shouldReturn500OnAuthFailureWith401() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 500 when un-Delegable origin service returns a 401",
                         HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }

        @Test
        public void shouldReturn500OnAuthFailureWith403() {
            when(response.getStatus()).thenReturn(403);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 500 when un-Delegable origin service returns a 403",
                         HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
        }

        @Test
        public void shouldReturn501OnAuthFailureWith501() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must return a 501 when un-Delegable origin service returns a 501",
                         HttpStatusCode.NOT_IMPLEMENTED, responseDirector.getResponseStatus());
        }
    }

    public static class WhenHandlingWhiteListNotInDelegatedMode extends TestParent {

        @Override
        protected boolean delegable() {
            return false;
        }

        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Test
        public void shouldPassUriOnWhiteList() {
            when(request.getRequestURI()).thenReturn("/v1.0/application.wadl");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must pass requests with uri on white list", FilterAction.PASS,
                         requestDirector.getFilterAction());
        }

        @Test
        public void shouldReturnForUriNotOnWhiteList() {
            when(request.getRequestURI()).thenReturn("?param=/v1.0/application.wadl");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must return requests with uri not on white list", FilterAction.RETURN,
                         requestDirector.getFilterAction());
        }
    }

    public static class WhenHandlingWhiteListInDelegatedMode extends TestParent {

        @Override
        protected boolean delegable() {
            return true;
        }

        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Test
        public void shouldPassUriOnWhiteList() {
            when(request.getRequestURI()).thenReturn("/v1.0/application.wadl");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must pass requests with uri on white list", FilterAction.PASS,
                         requestDirector.getFilterAction());
        }

        @Test
        public void shouldPassUriNotOnWhiteListAsNonAuthedRequest() {
            when(request.getRequestURI()).thenReturn("?param=/v1.0/application.wadl");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must pass requests with uri not on white list when in delegated mode",
                         FilterAction.PASS, requestDirector.getFilterAction());
        }
    }

}
