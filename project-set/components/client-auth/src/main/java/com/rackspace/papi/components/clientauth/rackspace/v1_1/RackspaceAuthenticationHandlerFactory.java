package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.v1_1.AuthenticationServiceClient;
import com.rackspace.auth.v1_1.AuthenticationServiceClientFactory;
import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.service.datastore.Datastore;

import java.util.List;
import java.util.regex.Pattern;

public class RackspaceAuthenticationHandlerFactory {
   public static AuthModule newInstance(ClientAuthConfig cfg, KeyedRegexExtractor accountRegexExtractor, Datastore datastore, List<Pattern> whiteListRegexPatterns) {
      final RackspaceAuth authConfig = cfg.getRackspaceAuth();
      final RackspaceUserInfoCache cache = new RackspaceUserInfoCache(datastore);

      final AuthenticationServiceClient serviceClient = new AuthenticationServiceClientFactory().buildAuthServiceClient(
              authConfig.getAuthenticationServer().getUri(), authConfig.getAuthenticationServer().getUsername(), authConfig.getAuthenticationServer().getPassword());
      return new RackspaceAuthenticationHandler(authConfig, serviceClient, accountRegexExtractor, cache, whiteListRegexPatterns);
   }
   
}
