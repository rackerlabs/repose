package org.openrepose.components.rackspace.authz
import com.rackspace.auth.openstack.AuthenticationService
import com.rackspace.papi.commons.util.http.CommonHttpHeader
import com.rackspace.papi.commons.util.http.HttpStatusCode
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader
import com.rackspace.papi.filter.logic.FilterAction
import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import org.openrepose.components.authz.rackspace.config.ServiceAdminRoles
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint
import org.openrepose.components.rackspace.authz.cache.CachedEndpoint
import org.openrepose.components.rackspace.authz.cache.EndpointListCache
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Endpoint
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RequestAuthorizationHandlerGroovyTest extends Specification {

    AuthenticationService authenticationService
    AuthenticateResponse authenticateResponse
    EndpointListCache endpointListCache
    ServiceEndpoint serviceEndpoint
    ServiceAdminRoles serviceAdminRoles
    FilterDirector filterDirector
    HttpServletRequest httpServletRequest
    RequestAuthorizationHandler requestAuthorizationHandler

    private static final String UNAUTHORIZED_TOKEN = "abcdef-abcdef-abcdef-abcdef", AUTHORIZED_TOKEN = "authorized", CACHED_TOKEN = "cached";
    private static final String PUBLIC_URL = "http://service.api.f.com/v1.1", REGION = "ORD", NAME = "Nova", TYPE = "compute";

    protected AuthenticationService mockedAuthService;
    protected RequestAuthorizationHandler handler;
    protected EndpointListCache mockedCache;
    protected HttpServletRequest mockedRequest;

    def setup() {
        authenticationService = Mock()
        authenticateResponse = Mock()
        endpointListCache = Mock()
        serviceEndpoint = Mock()
        serviceAdminRoles = Mock()
        filterDirector = new FilterDirectorImpl()
        httpServletRequest = mock(HttpServletRequest.class)
        requestAuthorizationHandler = Mock()

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

        handler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, myServiceEndpoint, null);

        mockedRequest = mock(HttpServletRequest.class);
    }

    def "auth should be bypassed if an x-roles header role matches within a configured list of service admin roles"() {
        given:
        when(httpServletRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("abc")
        when(httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString())).thenReturn(Collections.enumeration(["role0", "role1", "role2"]))
        serviceAdminRoles.getServiceAdminRole() >> new ArrayList<String>()
        serviceAdminRoles.getServiceAdminRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService, endpointListCache,
                serviceEndpoint, serviceAdminRoles)

        when:
        requestAuthorizationHandler.authorizeRequest(filterDirector, httpServletRequest)

        then:
        filterDirector.getFilterAction() == FilterAction.PASS
    }

    def "auth should not be bypassed if the x-roles header role does not match within a configured list of service admin roles"() {
        given:
        when(httpServletRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("abc")
        when(httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString())).thenReturn(Collections.enumeration(["role0", "role2"]))
        serviceAdminRoles.getServiceAdminRole() >> new ArrayList<String>()
        serviceAdminRoles.getServiceAdminRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService, endpointListCache,
                serviceEndpoint, serviceAdminRoles)

        when:
        requestAuthorizationHandler.authorizeRequest(filterDirector, httpServletRequest)

        then:
        filterDirector.getFilterAction() != FilterAction.PASS
    }

    def "should Reject Delegated Authentication"() {
        when:
        when(mockedRequest.getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString())).thenReturn("Confirmed");

        final FilterDirector director = handler.handleRequest(mockedRequest, null);

        then:
        assertEquals("Authorization component must return requests that have had authentication delegated", FilterAction.RETURN, director.getFilterAction());
        assertEquals("Authorization component must reject delegated authentication with a 403", HttpStatusCode.FORBIDDEN, director.getResponseStatus());
    }

    def "should Reject Requests Without Auth Tokens"() {
        when:
        final FilterDirector director = handler.handleRequest(mockedRequest, null);

        then:
        assertEquals("Authorization component must return requests that do not have auth tokens", FilterAction.RETURN, director.getFilterAction());
        assertEquals("Authorization component must reject unauthenticated requests with a 401", HttpStatusCode.UNAUTHORIZED, director.getResponseStatus());
    }

    def "should Reject Unauthorized Requests"() {
        when:
        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(UNAUTHORIZED_TOKEN);

        final FilterDirector director = handler.handleRequest(mockedRequest, null);

        then:
        assertEquals("Authorization component must return unauthorized requests", FilterAction.RETURN, director.getFilterAction());
        assertEquals("Authorization component must reject unauthorized requests with a 403", HttpStatusCode.FORBIDDEN, director.getResponseStatus());
    }

    def "should Pass Authorized Requests"() {
        when:
        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);

        final FilterDirector director = handler.handleRequest(mockedRequest, null);

        then:
        assertEquals("Authorization component must pass authorized requests", FilterAction.PASS, director.getFilterAction());
    }

    def "should Return 500"() {
        when:
        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);
        when(mockedAuthService.getEndpointsForToken(AUTHORIZED_TOKEN)).thenThrow(new RuntimeException("Service Exception"));

        final FilterDirector director = handler.handleRequest(mockedRequest, null);

        then:
        assertEquals("Authorization component must retrun 500 on service exception", HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), director.getResponseStatus().intValue());
    }

/*
    def "should Cache Fresh Endpoint Lists"() {
        when:
        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);
        handler.handleRequest(mockedRequest, null);

        then:
        verify(mockedCache, times(1)).getCachedEndpointsForToken(AUTHORIZED_TOKEN);
        verify(mockedAuthService, times(1)).getEndpointsForToken(AUTHORIZED_TOKEN);
        verify(mockedCache, times(1)).cacheEndpointsForToken(eq(AUTHORIZED_TOKEN), any(List.class));
    }

    def "should Use Cache For Cached Endpoint Lists"() {
        when:
        when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(CACHED_TOKEN);
        handler.handleRequest(mockedRequest, null);

        then:
        verify(mockedCache, times(1)).getCachedEndpointsForToken(CACHED_TOKEN);
        verify(mockedAuthService, never()).getEndpointsForToken(CACHED_TOKEN);
        verify(mockedCache, never()).cacheEndpointsForToken(eq(AUTHORIZED_TOKEN), any(List.class));
    }
*/
}
