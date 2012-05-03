package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.AuthenticationServiceClient;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.openstack.config.OpenStackIdentityService;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.service.datastore.Datastore;

public final class OpenStackAuthenticationHandlerFactory {

   private OpenStackAuthenticationHandlerFactory() {}
   
   public static AuthModule newInstance(ClientAuthConfig config, KeyedRegexExtractor accountRegexExtractor, Datastore datastore, UriMatcher uriMatcher) {
      final OpenStackUserInfoCache cache = new OpenStackUserInfoCache(datastore);
      final OpenstackAuth authConfig = config.getOpenstackAuth();
      final OpenStackIdentityService ids = authConfig.getIdentityService();
      final OpenStackAuthenticationService authService = new AuthenticationServiceClient(ids.getUri(), ids.getUsername(), ids.getPassword());

      return new OpenStackAuthenticationHandler(authConfig, authService, accountRegexExtractor, cache, uriMatcher);
   }   
}
