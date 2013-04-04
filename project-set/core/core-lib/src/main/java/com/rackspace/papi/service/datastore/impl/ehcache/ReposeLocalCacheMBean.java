package com.rackspace.papi.service.datastore.impl.ehcache;

public interface ReposeLocalCacheMBean {
    String OBJECT_NAME = "com.rackspace.papi.service.datastore.impl.ehcache:type=ReposeLocalCache";

    boolean removeTokenAndRoles(String tenantId, String token);

    boolean removeGroups(String tenantId, String token);

    boolean removeLimits(String userId);

    void removeAllCacheData();
}
