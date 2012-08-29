package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component("reposeLocalDatastore")
@ManagedResource(objectName = "com.rackspace.papi.service.datastore.impl.ehcache:type=ReposeLocalCache", description="Repose local datastore MBean.")
public class ReposeLocalCache implements ReposeLocalCacheMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeLocalCache.class);
    private static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
    private static final String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";
    private final DatastoreService datastoreService;

    @Autowired
    public ReposeLocalCache(@Qualifier("datastoreService") DatastoreService datastoreService) {
        this.datastoreService = datastoreService;
    }
    
    private String getCacheKey(String tenantId, String token) {
        
        String key;
        if (StringUtilities.isNotBlank(tenantId)) {
            key = tenantId + ":" + token;
        } else {
            key = token;
        }
        
        return key;
    }

    @Override
    @ManagedOperation
    public boolean removeTokenAndRoles(String tenantId, String token) {
        boolean removed = datastoreService.defaultDatastore().getDatastore().remove(AUTH_TOKEN_CACHE_PREFIX + "." + getCacheKey(tenantId, token));
        
        LOG.info("Removed token from cache: " + removed + (StringUtilities.isNotBlank(tenantId)? " (" + tenantId + ")": ""));
        
        return removed;
    }

    @Override
    @ManagedOperation
    public boolean removeGroups(String tenantId, String token) {
        boolean removed = datastoreService.defaultDatastore().getDatastore().remove(AUTH_GROUP_CACHE_PREFIX + "." + getCacheKey(tenantId, token));
        
        LOG.info("Removed groups from cache: " + removed + (StringUtilities.isNotBlank(tenantId)? " (" + tenantId + ")": ""));
        
        return removed;
    }
}
