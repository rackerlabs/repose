package com.rackspace.papi.components.clientauth.common

import com.rackspace.auth.AuthGroup
import com.rackspace.auth.AuthGroups
import com.rackspace.auth.AuthToken
import com.rackspace.papi.commons.util.regex.ExtractorResult
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.filter.logic.FilterDirector
import spock.lang.Specification

class AuthenticationHandlerTest extends Specification {

    Configurables configurables = Mock()
    EndpointsConfiguration endpointsConfiguration = Mock()
    AuthenticationHandler authenticationHandler

    def "when using the safeEndpointsTtl method, it should not allow for numbers larger than the integer limit"() {
        given:
        configurables.getEndpointsConfiguration() >> endpointsConfiguration
        endpointsConfiguration.getCacheTimeout() >> Long.valueOf(Integer.MAX_VALUE) + 1
        authenticationHandler = new TestableAuthenticationHandler(configurables)

        when:
        def i = authenticationHandler.safeEndpointsTtl()

        then:
        i == Integer.MAX_VALUE
    }

    def "when checking the cache for endpoints, should return null if not present"() {
        given:
        configurables.getEndpointsConfiguration() >> endpointsConfiguration
        endpointsConfiguration.getCacheTimeout() >> Long.valueOf(Integer.MAX_VALUE) + 1
        authenticationHandler = new TestableAuthenticationHandler(configurables)
        String token = null;

        when:
        def nil = authenticationHandler.checkEndpointsCache(token)

        then:
        nil == null;
    }
}

class TestableAuthenticationHandler extends AuthenticationHandler {

    TestableAuthenticationHandler(Configurables configurables) {
        super(configurables, null, null, null, null)
    }

    @Override
    Integer safeEndpointsTtl() {
        super.safeEndpointsTtl()
    }

    TestableAuthenticationHandler(Configurables configurables, AuthTokenCache cache, AuthGroupCache grpCache,
                                  EndpointsCache endpointsCache, UriMatcher uriMatcher) {
        super(configurables, cache, grpCache, endpointsCache, uriMatcher)
    }

    @Override
    protected AuthToken validateToken(ExtractorResult<String> account, String token) {
        return null
    }

    @Override
    protected AuthGroups getGroups(String group) {
        return null
    }

    @Override
    protected String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration) {
        return null
    }

    @Override
    protected FilterDirector processResponse(ReadableHttpServletResponse response) {
        return null
    }

    @Override
    protected void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable, FilterDirector filterDirector, String extractedResult, List<AuthGroup> groups, String endpointsBase64) {

    }

    @Override
    protected String checkEndpointsCache(String token) {
        super.checkEndpointsCache()
    }
}
