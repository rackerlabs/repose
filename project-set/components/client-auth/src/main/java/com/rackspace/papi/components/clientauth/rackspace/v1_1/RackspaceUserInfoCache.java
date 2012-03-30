package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.v1_1.CachableTokenInfo;
import com.rackspace.papi.components.clientauth.UserAuthTokenCache;
import com.rackspace.papi.service.datastore.Datastore;

public class RackspaceUserInfoCache extends UserAuthTokenCache<CachableTokenInfo> {
   public final static String AUTH_TOKEN_CACHE_PREFIX = "rackspace.v1.1.token";

   public RackspaceUserInfoCache(Datastore store) {
      super(store, CachableTokenInfo.class);
   }
   
   @Override
   public String getCachePrefix() {
      return AUTH_TOKEN_CACHE_PREFIX;
   }

   @Override
   public boolean validateToken(CachableTokenInfo cachedValue, String passedValue) {
      return cachedValue != null && cachedValue.getTokenId() != null && cachedValue.safeTokenTtl() > 0 && cachedValue.getTokenId().equals(passedValue);
   }
   
}
