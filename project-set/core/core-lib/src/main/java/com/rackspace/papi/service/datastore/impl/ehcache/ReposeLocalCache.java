package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.DatastoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component("reposeLocalDatastore")
@ManagedResource(objectName = "com.rackspace.papi.service.datastore.impl.ehcache:type=ReposeLocalCache", description="Repose local datastore MBean.")
public class ReposeLocalCache implements ReposeLocalCacheMBean {

    private static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
    private static final String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";
    private final DatastoreService datastoreService;

    @Autowired
    public ReposeLocalCache(@Qualifier("datastoreService") DatastoreService datastoreService) {
        this.datastoreService = datastoreService;
    }

    @Override
    @ManagedOperation
    public boolean removeTokenAndRoles(String key) {
        return datastoreService.defaultDatastore().getDatastore().remove(AUTH_TOKEN_CACHE_PREFIX + "." + key);
    }

    @Override
    @ManagedOperation
    public boolean removeGroups(String key) {
        return datastoreService.defaultDatastore().getDatastore().remove(AUTH_GROUP_CACHE_PREFIX + "." + key);
    }
}
