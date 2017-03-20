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
package org.openrepose.core.services.datastore.impl.ehcache;

import com.codahale.metrics.ehcache.InstrumentedEhcache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreManager;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public class EHCacheDatastoreManager implements DatastoreManager {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(EHCacheDatastoreManager.class);

    private static final String CACHE_NAME_PREFIX = "PAPI_LOCAL";
    private static final String CACHE_MANAGER_NAME = "LocalDatastoreCacheManager";
    private final CacheManager cacheManagerInstance;
    private Ehcache cache;

    public EHCacheDatastoreManager(Optional<MetricsService> metricsService) {

        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName(CACHE_MANAGER_NAME);
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        this.cacheManagerInstance = CacheManager.newInstance(defaultConfiguration);

        String cacheName = CACHE_NAME_PREFIX + ":" + cacheManagerInstance.getName() + UUID.randomUUID().toString();

        final Ehcache cacheOrig = new Cache(cacheName, 20000, false, false, 5, 2);
        cacheManagerInstance.addCache(cacheOrig);

        this.cache = metricsService.map(metrics -> InstrumentedEhcache.instrument(metrics.getRegistry(), cacheOrig))
                .orElse(cacheOrig);
    }

    @Override
    public Datastore getDatastore() {
        return new EHCacheDatastore(cache);
    }

    @Override
    public boolean isDistributed() {
        return false;
    }

    @Override
    public void destroy() {
        try {
            if (cacheManagerInstance != null) {
                cacheManagerInstance.removalAll();
                cacheManagerInstance.shutdown();
            }
        } catch (Exception e) {
            LOG.warn("Error occurred when shutting down datastore: " + e.getMessage(), e);
        }
    }
}
