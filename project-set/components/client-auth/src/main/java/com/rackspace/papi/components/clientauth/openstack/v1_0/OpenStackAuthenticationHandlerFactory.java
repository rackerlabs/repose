package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.AuthenticationService;
import com.rackspace.auth.openstack.AuthenticationServiceFactory;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.common.AuthGroupCache;
import com.rackspace.papi.components.clientauth.common.AuthTokenCache;
import com.rackspace.papi.components.clientauth.common.AuthenticationHandler;
import com.rackspace.papi.components.clientauth.common.Configurables;
import com.rackspace.papi.components.clientauth.common.EndpointsCache;
import com.rackspace.papi.components.clientauth.common.EndpointsConfiguration;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.openstack.config.OpenStackIdentityService;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.service.datastore.Datastore;

public final class OpenStackAuthenticationHandlerFactory {
    private static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
    private static final String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";
    private static final String ENDPOINTS_CACHE_PREFIX = "openstack.endpoints.cache";

    private OpenStackAuthenticationHandlerFactory() {
    }

    public static AuthenticationHandler newInstance(ClientAuthConfig config, KeyedRegexExtractor accountRegexExtractor, Datastore datastore, UriMatcher uriMatcher) {
        final AuthTokenCache cache = new AuthTokenCache(datastore, AUTH_TOKEN_CACHE_PREFIX);
        final AuthGroupCache grpCache = new AuthGroupCache(datastore, AUTH_GROUP_CACHE_PREFIX);
        final EndpointsCache endpointsCache = new EndpointsCache(datastore, ENDPOINTS_CACHE_PREFIX);
        final OpenstackAuth authConfig = config.getOpenstackAuth();
        final OpenStackIdentityService ids = authConfig.getIdentityService();
        final EndpointsConfiguration endpointsConfiguration;
        final AuthenticationService authService = new AuthenticationServiceFactory().build(ids.getUri(),
                                                                                           ids.getUsername(),
                                                                                           ids.getPassword(),
                                                                                           ids.getTenantId());

        //null check to prevent NPE when accessing config element attributes
        if (authConfig.getEndpointsInHeader() != null) {
            endpointsConfiguration = new EndpointsConfiguration(authConfig.getEndpointsInHeader().getFormat(),
                                                                authConfig.getEndpointsInHeader().getCacheTimeout(),
                                                                authConfig.getEndpointsInHeader()
                                                                        .getIdentityContractVersion().intValue());
        } else {
            endpointsConfiguration = null;
        }

        final Configurables configurables = new Configurables(authConfig.isDelegable(),
                                                              ids.getUri(),
                                                              accountRegexExtractor,
                                                              authConfig.isTenanted(),
                                                              authConfig.getGroupCacheTimeout(),
                                                              authConfig.getTokenCacheTimeout(),
                                                              authConfig.isRequestGroups(),
                                                              endpointsConfiguration);

        return new OpenStackAuthenticationHandler(configurables, authService, cache, grpCache, endpointsCache, uriMatcher);
    }
}
