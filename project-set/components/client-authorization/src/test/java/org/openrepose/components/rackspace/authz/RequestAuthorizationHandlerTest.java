package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.AuthenticationService;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint;
import org.openrepose.components.rackspace.authz.cache.CachedEndpoint;
import org.openrepose.components.rackspace.authz.cache.EndpointListCache;
import org.openstack.docs.identity.api.v2.Endpoint;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RequestAuthorizationHandlerTest {

    private static final String UNAUTHORIZED_TOKEN = "abcdef-abcdef-abcdef-abcdef", AUTHORIZED_TOKEN = "authorized", CACHED_TOKEN = "cached";
    private static final String PUBLIC_URL = "http://service.api.f.com/v1.1", REGION = "ORD", NAME = "Nova", TYPE = "compute";

    @Ignore
    public static class TestParent {

        protected AuthenticationService mockedAuthService;
        protected RequestAuthorizationHandler handler;
        protected EndpointListCache mockedCache;
        protected HttpServletRequest mockedRequest;

        @Before
        public void beforeAny() {
            // Caching mocks
            mockedCache = mock(EndpointListCache.class);

            final List<CachedEndpoint> cachedEndpointList = new LinkedList<CachedEndpoint>();
            cachedEndpointList.add(new CachedEndpoint(PUBLIC_URL, REGION, NAME, TYPE));

            when(mockedCache.getCachedEndpointsForToken(AUTHORIZED_TOKEN)).thenReturn(null);
            when(mockedCache.getCachedEndpointsForToken(CACHED_TOKEN)).thenReturn(cachedEndpointList);

            // Auth service mocks
            final List<Endpoint> endpointList = new LinkedList<Endpoint>();

            Endpoint endpoint = new Endpoint();
            endpoint.setPublicURL(PUBLIC_URL);
            endpoint.setRegion(REGION);
            endpoint.setName(NAME);
            endpoint.setType(TYPE);

            // Added to test case where endpoint values are null
            Endpoint endpointb = new Endpoint();

            endpointList.add(endpointb);
            endpointList.add(endpoint);

            mockedAuthService = mock(AuthenticationService.class);
            when(mockedAuthService.getEndpointsForToken(UNAUTHORIZED_TOKEN)).thenReturn(Collections.EMPTY_LIST);
            when(mockedAuthService.getEndpointsForToken(AUTHORIZED_TOKEN)).thenReturn(endpointList);
            when(mockedAuthService.getEndpointsForToken(CACHED_TOKEN)).thenReturn(endpointList);

            final ServiceEndpoint myServiceEndpoint = new ServiceEndpoint();
            myServiceEndpoint.setHref(PUBLIC_URL);
            myServiceEndpoint.setRegion(REGION);
            myServiceEndpoint.setName(NAME);
            myServiceEndpoint.setType(TYPE);

            handler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, myServiceEndpoint);

            mockedRequest = mock(HttpServletRequest.class);
        }
    }

    public static class WhenAuthorizingRequests extends TestParent {

        @Test
        public void shouldRejectDelegatedAuthentication() {
            when(mockedRequest.getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString())).thenReturn("Confirmed");

            final FilterDirector director = handler.handleRequest(mockedRequest, null);

            assertEquals("Authorization component must return requests that have had authentication delegated", FilterAction.RETURN, director.getFilterAction());
            assertEquals("Authorization component must reject delegated authentication with a 403", HttpStatusCode.FORBIDDEN, director.getResponseStatus());
        }

        @Test
        public void shouldRejectRequestsWithoutAuthTokens() {
            final FilterDirector director = handler.handleRequest(mockedRequest, null);

            assertEquals("Authorization component must return requests that do not have auth tokens", FilterAction.RETURN, director.getFilterAction());
            assertEquals("Authorization component must reject unauthenticated requests with a 401", HttpStatusCode.UNAUTHORIZED, director.getResponseStatus());
        }

        @Test
        public void shouldRejectUnauthorizedRequests() {
            when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(UNAUTHORIZED_TOKEN);

            final FilterDirector director = handler.handleRequest(mockedRequest, null);

            assertEquals("Authorization component must return unauthorized requests", FilterAction.RETURN, director.getFilterAction());
            assertEquals("Authorization component must reject unauthorized requests with a 403", HttpStatusCode.FORBIDDEN, director.getResponseStatus());
        }

        @Test
        public void shouldPassAuthorizedRequests() {
            when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);

            final FilterDirector director = handler.handleRequest(mockedRequest, null);

            assertEquals("Authorization component must pass authorized requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldReturn500(){
            when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);
            when(mockedAuthService.getEndpointsForToken(AUTHORIZED_TOKEN)).thenThrow(new RuntimeException("Service Exception"));

            final FilterDirector director = handler.handleRequest(mockedRequest, null);

            assertEquals("Authorization component must retrun 500 on service exception", HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), director.getResponseStatus().intValue());
        }
    }

    public static class WhenAuthorizingRequestsAgainstCachedEndpointLists extends TestParent {

        @Test
        public void shouldCacheFreshEndpointLists() throws Exception {
            when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);
            handler.handleRequest(mockedRequest, null);

            verify(mockedCache, times(1)).getCachedEndpointsForToken(AUTHORIZED_TOKEN);
            verify(mockedAuthService, times(1)).getEndpointsForToken(AUTHORIZED_TOKEN);
            verify(mockedCache, times(1)).cacheEndpointsForToken(eq(AUTHORIZED_TOKEN), any(List.class));
        }

        @Test
        public void shouldUseCacheForCachedEndpoitnLists() throws Exception {
            when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(CACHED_TOKEN);
            handler.handleRequest(mockedRequest, null);

            verify(mockedCache, times(1)).getCachedEndpointsForToken(CACHED_TOKEN);
            verify(mockedAuthService, never()).getEndpointsForToken(CACHED_TOKEN);
            verify(mockedCache, never()).cacheEndpointsForToken(eq(AUTHORIZED_TOKEN), any(List.class));
        }
    }
}
