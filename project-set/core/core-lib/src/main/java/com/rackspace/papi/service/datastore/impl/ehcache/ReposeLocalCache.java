package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;

@Component("reposeLocalDatastore")
@ManagedResource(objectName = "com.rackspace.papi.service.datastore.impl.ehcache:type=ReposeLocalCache",
                 description = "Repose local datastore MBean.")
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

    private String getEncodedUserCacheKey(String user) throws NoSuchAlgorithmException {
        final byte[] hashBytes =
                MD5MessageDigestFactory.getInstance().newMessageDigest().digest(user.getBytes(CharacterSets.UTF_8));

        return UUIDEncodingProvider.getInstance().encode(hashBytes);
    }

    @Override
    @ManagedOperation
    public boolean removeTokenAndRoles(String tenantId, String token) {
        boolean removed = datastoreService.defaultDatastore().getDatastore()
                .remove(AUTH_TOKEN_CACHE_PREFIX + "." + getCacheKey(tenantId, token));

        LOG.info("Removed token from cache: " + removed +
                         (StringUtilities.isNotBlank(tenantId) ? " (" + tenantId + ")" : ""));

        return removed;
    }

    @Override
    @ManagedOperation
    public boolean removeGroups(String tenantId, String token) {
        boolean removed = datastoreService.defaultDatastore().getDatastore()
                .remove(AUTH_GROUP_CACHE_PREFIX + "." + getCacheKey(tenantId, token));

        LOG.info("Removed groups from cache: " + removed +
                         (StringUtilities.isNotBlank(tenantId) ? " (" + tenantId + ")" : ""));

        return removed;
    }

    @Override
    @ManagedOperation
    public boolean removeLimits(String userId) {
        boolean removed = removeWithUnencodedUser(userId);

        if (!removed) {
            removed = removeWithEncodedUser(userId);
        }

        return removed;
    }

    @Override
    @ManagedOperation
    public void removeAllCacheData() {
        datastoreService.getDatastore(DatastoreService.DEFAULT_LOCAL).getDatastore().removeAllCacheData();
    }

    private boolean removeWithUnencodedUser(String userId) {
        return datastoreService.defaultDatastore().getDatastore().remove(userId);
    }

    private boolean removeWithEncodedUser(String userId) {
        boolean removed = false;

        try {
            removed = datastoreService.defaultDatastore().getDatastore().remove(getEncodedUserCacheKey(userId));

            LOG.info("Removed rate limits from cache: " + removed +
                             (StringUtilities.isNotBlank(userId) ? " (" + userId + ")" : ""));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Your instance of the Java Runtime Environment does not support the MD5 hash algorithm.", e);
        }

        return removed;
    }
}
