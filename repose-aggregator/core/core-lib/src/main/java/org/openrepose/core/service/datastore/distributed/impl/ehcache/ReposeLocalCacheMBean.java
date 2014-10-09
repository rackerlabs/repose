package org.openrepose.core.service.datastore.distributed.impl.ehcache;

public interface ReposeLocalCacheMBean {
    String OBJECT_NAME = "org.openrepose.core.service.datastore.impl.ehcache:type=ReposeLocalCache";

    boolean removeTokenAndRoles(String tenantId, String token);

    boolean removeGroups(String tenantId, String token);

    boolean removeLimits(String userId);

    void removeAllCacheData();
}
