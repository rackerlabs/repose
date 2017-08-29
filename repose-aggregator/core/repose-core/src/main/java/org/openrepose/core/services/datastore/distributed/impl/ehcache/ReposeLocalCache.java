/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.distributed.impl.ehcache;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.hash.MD5MessageDigestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

@Named("reposeLocalDatastore")
@ManagedResource(description = "Repose local datastore MBean.")
public class ReposeLocalCache implements ReposeLocalCacheMBean {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeLocalCache.class);
    private static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
    private static final String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";
    private final DatastoreService datastoreService;

    @Inject
    public ReposeLocalCache(DatastoreService datastoreService) {
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
                MD5MessageDigestFactory.getInstance().newMessageDigest().digest(user.getBytes(StandardCharsets.UTF_8));

        return UUIDEncodingProvider.getInstance().encode(hashBytes);
    }

    @Override
    @ManagedOperation
    public boolean removeTokenAndRoles(String tenantId, String token) {
        boolean removed = datastoreService.getDefaultDatastore()
                .remove(AUTH_TOKEN_CACHE_PREFIX + "." + getCacheKey(tenantId, token));

        LOG.info("Removed token from cache: " + removed +
                (StringUtilities.isNotBlank(tenantId) ? " (" + tenantId + ")" : ""));

        return removed;
    }

    @Override
    @ManagedOperation
    public boolean removeGroups(String tenantId, String token) {
        boolean removed = datastoreService.getDefaultDatastore()
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
        datastoreService.getDefaultDatastore().removeAll();
    }

    private boolean removeWithUnencodedUser(String userId) {
        return datastoreService.getDefaultDatastore().remove(userId);
    }

    private boolean removeWithEncodedUser(String userId) {
        boolean removed = false;

        try {
            removed = datastoreService.getDefaultDatastore().remove(getEncodedUserCacheKey(userId));

            LOG.info("Removed rate limits from cache: " + removed +
                    (StringUtilities.isNotBlank(userId) ? " (" + userId + ")" : ""));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Your instance of the Java Runtime Environment does not support the MD5 hash algorithm.", e);
        }

        return removed;
    }
}
