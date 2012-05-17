package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthToken;
import com.rackspace.papi.components.clientauth.common.UserAuthTokenCache;
import com.rackspace.papi.service.datastore.Datastore;

public class OpenStackUserInfoCache extends UserAuthTokenCache<AuthToken> {
   public static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
   
   public OpenStackUserInfoCache(Datastore store) {
      super(store, AuthToken.class);
   }

   @Override
   public String getCachePrefix() {
      return AUTH_TOKEN_CACHE_PREFIX;
   }

   @Override
   public boolean validateToken(AuthToken cachedValue, String passedValue) {
      return cachedValue != null && cachedValue.getTokenId() != null && cachedValue.safeTokenTtl() > 0 && cachedValue.getTokenId().equals(passedValue);
   }
}
