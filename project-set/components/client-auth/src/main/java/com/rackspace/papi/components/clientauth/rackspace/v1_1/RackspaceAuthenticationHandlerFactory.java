package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.rackspace.AuthenticationService;
import com.rackspace.auth.rackspace.AuthenticationServiceFactory;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.common.AuthGroupCache;
import com.rackspace.papi.components.clientauth.common.AuthTokenCache;
import com.rackspace.papi.components.clientauth.common.AuthenticationHandler;
import com.rackspace.papi.components.clientauth.common.Configurables;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.service.datastore.Datastore;

public final class RackspaceAuthenticationHandlerFactory {
    private static final String AUTH_TOKEN_CACHE_PREFIX = "rackspace.v1.1.token";
    private static final String AUTH_GROUP_CACHE_PREFIX = "rackspace.v1.1.group";

    private RackspaceAuthenticationHandlerFactory() {
    }

    public static AuthenticationHandler newInstance(ClientAuthConfig cfg, KeyedRegexExtractor accountRegexExtractor, Datastore datastore, UriMatcher uriMatcher) {
        final RackspaceAuth authConfig = cfg.getRackspaceAuth();
        final AuthTokenCache cache = new AuthTokenCache(datastore, AUTH_TOKEN_CACHE_PREFIX);
        final AuthGroupCache grpCache = new AuthGroupCache(datastore, AUTH_GROUP_CACHE_PREFIX);

        final AuthenticationService serviceClient = new AuthenticationServiceFactory().build(
                authConfig.getAuthenticationServer().getUri(), authConfig.getAuthenticationServer().getUsername(), authConfig.getAuthenticationServer().getPassword());

        final Configurables configurables = new Configurables(authConfig.isDelegable(),
                                                              authConfig.getAuthenticationServer().getUri(),
                                                              accountRegexExtractor,
                                                              //Auth v1.1 will always check tenant against the passed token.
                                                              true,
                                                              authConfig.getGroupCacheTimeout(),
                                                              authConfig.getTokenCacheTimeout(),
                                                              authConfig.isRequestGroups(),
                                                              null); //null because this is not implemented in RS auth

        return new RackspaceAuthenticationHandler(configurables, serviceClient, cache, grpCache, null, uriMatcher);
    }

}
