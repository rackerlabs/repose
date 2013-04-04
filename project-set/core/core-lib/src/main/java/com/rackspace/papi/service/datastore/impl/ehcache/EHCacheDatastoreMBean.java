package com.rackspace.papi.service.datastore.impl.ehcache;

public interface EHCacheDatastoreMBean {
    String OBJECT_NAME = "com.rackspace.papi.service.datastore.impl.ehcache:type=EHCacheDatastore";

    void removeAllCachedData();
}
