package com.rackspace.papi.commons.util.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;


public class ReposeMBeanObjectNames {
    private static final String REPOSE_EH_CACHE_OBJECT_NAME = "com.rackspace.papi.service.datastore.impl.ehcache:type=ReposeEHCache";
    private ObjectName reposeEHCache;

    public ReposeMBeanObjectNames() {

        try {
            this.reposeEHCache = new ObjectName(REPOSE_EH_CACHE_OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Exception when creating " + REPOSE_EH_CACHE_OBJECT_NAME + ": " + e.getMessage());
        }
    }

    public ObjectName getReposeEHCache() {
        return reposeEHCache;
    }
}
