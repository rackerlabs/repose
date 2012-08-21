package com.rackspace.papi.service.datastore.impl.ehcache;

public interface ReposeEHCacheMBean {

    boolean removeTokenAndRoles(String key);
    boolean removeGroups(String key);
}
