package org.openrepose.filters.authz
import com.rackspace.httpdelegation.HttpDelegationHeaderNames
import com.rackspace.httpdelegation.JavaDelegationManagerProxy
import org.openrepose.common.auth.openstack.AuthenticationService
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.OpenStackServiceHeader
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.components.authz.rackspace.config.DelegatingType
import org.openrepose.components.authz.rackspace.config.IgnoreTenantRoles
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.filters.authz.cache.CachedEndpoint
import org.openrepose.filters.authz.cache.EndpointListCache
import org.openstack.docs.identity.api.v2.Endpoint
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*
import static org.openrepose.core.filter.logic.FilterAction.PASS
import static org.openrepose.core.filter.logic.FilterAction.RETURN

class RequestAuthorizationHandlerTest extends Specification {

    AuthenticationService authenticationService
    EndpointListCache endpointListCache
    ServiceEndpoint serviceEndpoint
    IgnoreTenantRoles ignoreTenantRoles
    RequestAuthorizationHandler requestAuthorizationHandler

    private static final String UNAUTHORIZED_TOKEN = "abcdef-abcdef-abcdef-abcdef", AUTHORIZED_TOKEN = "authorized", CACHED_TOKEN = "cached";
    private static final String PUBLIC_URL = "http://service.api.f.com/v1.1", REGION = "ORD", NAME = "Nova", TYPE = "compute";

    protected AuthenticationService mockedAuthService
    protected RequestAuthorizationHandler handler
    protected EndpointListCache mockedCache
    MockHttpServletRequest mockedRequest

    def setup() {
        mockedRequest = new MockHttpServletRequest();
        authenticationService = Mock()
        endpointListCache = Mock()
        ignoreTenantRoles = new IgnoreTenantRoles()

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

        serviceEndpoint = new ServiceEndpoint().with {
            href = PUBLIC_URL
            region = REGION
            name = NAME
            type = TYPE
            it
        }

        handler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, serviceEndpoint, null, null);
    }

    @Unroll
    def "#desc auth should be bypassed if an x-roles header role matches within a configured list of service admin roles"() {
        given:
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), "abc")
        mockedRequest.addHeader(OpenStackServiceHeader.ROLES.toString(), ["role0", "role1", "role2"])
        ignoreTenantRoles.getIgnoreTenantRole() >> new ArrayList<String>()
        ignoreTenantRoles.getIgnoreTenantRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, endpointListCache,
                serviceEndpoint, ignoreTenantRoles, delegable)

        when:
        def filterDirector = requestAuthorizationHandler.handleRequest(mockedRequest, null)

        then:
        filterDirector.getFilterAction() == PASS
        filterDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap(HttpDelegationHeaderNames.Delegated())) == null

        where:
        desc                   | delegable
        "With delegable on,"   | new DelegatingType().with { it.quality = 0.3; it }
        "With delegable off, " | null

    }

    @Unroll
    def "#desc bypassed if the x-roles header role does not match within a configured list of service admin roles"() {
        given:
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), AUTHORIZED_TOKEN)
        mockedRequest.addHeader(OpenStackServiceHeader.ROLES.toString(), ["role0", "role2"])
        ignoreTenantRoles.getIgnoreTenantRole() >> new ArrayList<String>()
        ignoreTenantRoles.getIgnoreTenantRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, endpointListCache,
                new ServiceEndpoint().with { it.href = "http://foo.com/moo"; it}, ignoreTenantRoles, delegable)

        when:
        def filterDirector = requestAuthorizationHandler.handleRequest(mockedRequest, null)

        then:
        filterDirector.getFilterAction() == filterAction
        filterDirector.getResponseStatusCode() == HttpServletResponse.SC_FORBIDDEN
        isDelegableHeaderAccurateFor delegable, filterDirector, serviceCatalogFailureMessage()

        where:
        desc                                         | filterAction | delegable
        "with delegable on, auth failures should be" | PASS         | new DelegatingType().with { it.quality = 0.3; it }
        "with delegable off, auth should not be"     | RETURN       | null
    }

    @Unroll
    def "#desc reject requests without auth tokens"() {
        given:
        def requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, serviceEndpoint, null, delegable);

        when:
        def director = requestAuthorizationHandler.handleRequest(mockedRequest, null);

        then:
        director.getFilterAction() == filterAction
        director.getResponseStatusCode() == HttpServletResponse.SC_UNAUTHORIZED
        isDelegableHeaderAccurateFor delegable, director, authTokenNotFoundMessage()

        where:
        desc                                    | filterAction | delegable
        "When delegating is not set, it should" | RETURN       | null
        "When delegating is set, it should not" | PASS         | new DelegatingType()
    }

    @Unroll
    def "#desc reject unauthorized requests"() {
        given:
        def requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, serviceEndpoint, null, delegable);
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), UNAUTHORIZED_TOKEN)

        when:
        final FilterDirector director = requestAuthorizationHandler.handleRequest(mockedRequest, null);

        then:
        director.getFilterAction() == filterAction
        director.getResponseStatusCode() == HttpServletResponse.SC_FORBIDDEN
        isDelegableHeaderAccurateFor delegable, director, authTokenNotAuthorized()

        where:
        desc                                    | filterAction | delegable
        "When delegating is not set, it should" | RETURN       | null
        "When delegating is set, it should not" | PASS         | new DelegatingType()
    }

    @Unroll
    def "#desc should pass authorized requests"() {
        given:
        def requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, serviceEndpoint, null, delegable);
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), AUTHORIZED_TOKEN)

        when:
        final FilterDirector director = requestAuthorizationHandler.handleRequest(mockedRequest, null);

        then:
        director.getFilterAction() == PASS
        director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(HttpDelegationHeaderNames.Delegated())) == null

        where:
        desc                                    | delegable
        "When delegating is not set, it should" | null
        "When delegating is set, it should not" | new DelegatingType()
    }

    @Unroll
    def "#desc return a 500 when the auth returns a service exception"() {
        given:
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), AUTHORIZED_TOKEN)
        when(mockedAuthService.getEndpointsForToken(AUTHORIZED_TOKEN)).thenThrow(new RuntimeException("Service Exception"));
        def requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, serviceEndpoint, null, delegable);

        when:
        final FilterDirector director = requestAuthorizationHandler.handleRequest(mockedRequest, null);

        then:
        director.getResponseStatusCode() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        director.getFilterAction() == filterAction
        isDelegableHeaderAccurateFor delegable, director, authServiceFailure()

        where:
        desc                                    | filterAction | delegable
        "When delegating is not set, it should" | RETURN       | null
        "When delegating is set, it should not" | PASS         | new DelegatingType()
    }

    def "should Cache Fresh Endpoint Lists"() {
        given:
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), AUTHORIZED_TOKEN)

        when:
        handler.handleRequest(mockedRequest, null);

        then:
        verify(mockedCache, times(1)).getCachedEndpointsForToken(AUTHORIZED_TOKEN) == null
        verify(mockedAuthService, times(1)).getEndpointsForToken(AUTHORIZED_TOKEN) == null
        verify(mockedCache, times(1)).cacheEndpointsForToken(eq(AUTHORIZED_TOKEN), any(List.class)) == null
    }

    def "should Use Cache For Cached Endpoint Lists"() {
        given:
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), CACHED_TOKEN)

        when:
        handler.handleRequest(mockedRequest, null);

        then:
        verify(mockedCache, times(1)).getCachedEndpointsForToken(CACHED_TOKEN) == null
        verify(mockedAuthService, never()).getEndpointsForToken(CACHED_TOKEN) == null
        verify(mockedCache, never()).cacheEndpointsForToken(eq(AUTHORIZED_TOKEN), any(List.class)) == null
    }

    @Unroll
    def "With #configuration configured and auth #endpoint should #outcome"() {
        given:
        def requestAuthorizationHandler = new RequestAuthorizationHandler(mockedAuthService, mockedCache, createConfiguredEndpoint(configUri, configRegion, configName, configType), null, null);
        List<Endpoint> endpointList = new LinkedList<Endpoint>();
        endpointList.add(createAuthEndpoint(endpointUri, endpointRegion, endpointName, endpointType))
        when(mockedAuthService.getEndpointsForToken(AUTHORIZED_TOKEN)).thenReturn(endpointList);
        mockedRequest.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), AUTHORIZED_TOKEN)

        when:
        FilterDirector director = requestAuthorizationHandler.handleRequest(mockedRequest, null);

        then:
        director.filterAction == outcome

        where:
        configuration    | configUri        | configRegion | configName | configType  | endpoint               | endpointUri       | endpointRegion | endpointName | endpointType | outcome
        "uri"            | PUBLIC_URL       | null         | null       | null        | "uri matches"          | PUBLIC_URL        | REGION         | NAME         | TYPE         | PASS
        "uri"            | "http://foo.com" | null         | null       | null        | "uri doesn't match"    | "http://bar.com"  | REGION         | NAME         | TYPE         | RETURN
        "uri and region" | PUBLIC_URL       | REGION       | null       | null        | "uri and region match" | PUBLIC_URL        | REGION         | NAME         | TYPE         | PASS
        "uri and region" | PUBLIC_URL       | "here"       | null       | null        | "region doesn't match" | PUBLIC_URL        | "there"        | NAME         | TYPE         | RETURN
        "uri and name"   | PUBLIC_URL       | null         | NAME       | null        | "uri and name match"   | PUBLIC_URL        | REGION         | NAME         | TYPE         | PASS
        "uri and name"   | PUBLIC_URL       | null         | "Bob"      | null        | "name doesn't match"   | PUBLIC_URL        | REGION         | "Billy"      | TYPE         | RETURN
        "uri and type"   | PUBLIC_URL       | null         | null       | TYPE        | "uri and type match"   | PUBLIC_URL        | REGION         | NAME         | TYPE         | PASS
        "uri and type"   | PUBLIC_URL       | null         | null       | "dangerous" | "type doesn't match"   | PUBLIC_URL        | REGION         | NAME         | "confident"  | RETURN
        "all"            | PUBLIC_URL       | REGION       | NAME       | TYPE        | "all match"            | PUBLIC_URL        | REGION         | NAME         | TYPE         | PASS
        "all"            | PUBLIC_URL       | "here"       | NAME       | TYPE        | "region doesn't match" | PUBLIC_URL        | "there"        | NAME         | TYPE         | RETURN
        "all"            | PUBLIC_URL       | REGION       | "Bob"      | TYPE        | "name doesn't match"   | PUBLIC_URL        | REGION         | "Billy"      | TYPE         | RETURN
        "all"            | PUBLIC_URL       | REGION       | NAME       | "dangerous" | "type doesn't match"   | PUBLIC_URL        | REGION         | NAME         | "confident"  | RETURN
    }

    ServiceEndpoint createConfiguredEndpoint(String configUri, String configRegion, String configName, String configType) {
        new ServiceEndpoint().with {
            href = configUri
            region = configRegion
            name = configName
            type = configType
            it
        }
    }

    Endpoint createAuthEndpoint(String authUri, String authRegion, String authName, String authType) {
        new Endpoint().with {
            publicURL = authUri
            region = authRegion
            name = authName
            type = authType
            it
        }
    }

    void isDelegableHeaderAccurateFor(DelegatingType delegable, FilterDirector filterDirector, String message) {
        assert filterDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap(HttpDelegationHeaderNames.Delegated()))?.getAt(0) ==
                (delegable ? JavaDelegationManagerProxy.buildDelegationHeaders(filterDirector.getResponseStatusCode(),
                        "client-authorization", message, delegable.getQuality()).get(HttpDelegationHeaderNames.Delegated()).get(0) : null)
    }

    String serviceCatalogFailureMessage() {
        "User token: authorized: The user's service catalog does not contain an endpoint that matches the endpoint configured " +
                "in openstack-authorization.cfg.xml: \"http://foo.com/moo\".  User not authorized to access service."
    }

    String authTokenNotFoundMessage() {
        "Authentication token not found in X-Auth-Token header. Rejecting request."
    }

    String authTokenNotAuthorized() {
        "User token: abcdef-abcdef-abcdef-abcdef: The user's service catalog does not contain an endpoint that matches the endpoint configured in " +
                "openstack-authorization.cfg.xml: \"http://service.api.f.com/v1.1\".  User not authorized to access service."
    }

    String authServiceFailure() {
        "Failure in authorization component"
    }
}
