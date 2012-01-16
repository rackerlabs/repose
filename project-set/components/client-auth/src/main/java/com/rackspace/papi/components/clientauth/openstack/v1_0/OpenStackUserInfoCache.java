package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableUserInfo;
import com.rackspace.papi.components.clientauth.UserAuthTokenCache;
import com.rackspace.papi.service.datastore.Datastore;

public class OpenStackUserInfoCache extends UserAuthTokenCache<CachableUserInfo> {
   public static String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
   
   public OpenStackUserInfoCache(Datastore store) {
      super(store, CachableUserInfo.class);
   }

   @Override
   public String getCachePrefix() {
      return AUTH_TOKEN_CACHE_PREFIX;
   }

   @Override
   public boolean validateToken(CachableUserInfo cachedValue, String passedValue) {
      return cachedValue != null && cachedValue.getTokenId() != null && cachedValue.getSafeTokenTtl() > 0 && cachedValue.getTokenId().equals(passedValue);
   }
}
