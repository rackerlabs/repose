package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableGroupInfo;
import com.rackspace.papi.components.clientauth.UserAuthGroupsCache;
import com.rackspace.papi.service.datastore.Datastore;

public class OpenStackGroupInfoCache extends UserAuthGroupsCache<CachableGroupInfo> {
   public final static String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";

   public OpenStackGroupInfoCache(Datastore store) {
      super(store, CachableGroupInfo.class);
   }

   @Override
   public String getCachePrefix() {
      return AUTH_GROUP_CACHE_PREFIX;
   }

   @Override
   public boolean validateGroup(CachableGroupInfo cachedValue) {
      return cachedValue != null && cachedValue.getGroupIds() != null && cachedValue.safeGroupTtl() > 0;
   }
}
