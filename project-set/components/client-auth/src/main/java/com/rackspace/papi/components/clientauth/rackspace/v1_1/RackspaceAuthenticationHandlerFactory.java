package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.v1_1.AuthenticationServiceClient;
import com.rackspace.auth.v1_1.AuthenticationServiceClientFactory;
import com.rackspace.papi.components.clientauth.AuthModule;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.service.datastore.Datastore;

public final class RackspaceAuthenticationHandlerFactory {

   private RackspaceAuthenticationHandlerFactory() {}
   
   public static AuthModule newInstance(ClientAuthConfig cfg, KeyedRegexExtractor accountRegexExtractor, Datastore datastore, UriMatcher uriMatcher) {
      final RackspaceAuth authConfig = cfg.getRackspaceAuth();
      final RackspaceUserInfoCache cache = new RackspaceUserInfoCache(datastore);

      final AuthenticationServiceClient serviceClient = new AuthenticationServiceClientFactory().buildAuthServiceClient(
              authConfig.getAuthenticationServer().getUri(), authConfig.getAuthenticationServer().getUsername(), authConfig.getAuthenticationServer().getPassword());
      return new RackspaceAuthenticationHandler(authConfig, serviceClient, accountRegexExtractor, cache, uriMatcher);
   }
   
}
