package com.rackspace.papi.service.datastore.impl.ehcache;

public interface ReposeLocalCacheMBean {
    static final String OBJECT_NAME = "com.rackspace.papi.service.datastore.impl.ehcache:type=ReposeLocalCache";
    boolean removeTokenAndRoles(String key);
    boolean removeGroups(String key);
}
