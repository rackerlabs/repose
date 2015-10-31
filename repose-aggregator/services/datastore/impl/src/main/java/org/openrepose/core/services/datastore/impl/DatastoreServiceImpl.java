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
package org.openrepose.core.services.datastore.impl;

import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.generic.GenericResourceConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreManager;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.datastore.impl.distributed.HashRingDatastoreManager;
import org.openrepose.core.services.datastore.impl.ehcache.EHCacheDatastoreManager;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named
public class DatastoreServiceImpl implements DatastoreService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DatastoreServiceImpl.class);

    private final Map<String, DatastoreManager> distributedManagers;
    private final EHCacheConfigurationListener ehCacheConfigurationListener = new EHCacheConfigurationListener();

    private DatastoreManager localDatastoreManager;
    private ConfigurationService configurationService;

    @Inject
    public DatastoreServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;

        this.localDatastoreManager = new EHCacheDatastoreManager(null);
        this.distributedManagers = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        configurationService.subscribeTo(
                "local-datastore",
                "ehcache.xml",
                ehCacheConfigurationListener,
                new GenericResourceConfigurationParser()
        );
    }

    @PreDestroy
    public void destroy() {
        LOG.info("Destroying datastore service context");
        configurationService.unsubscribeFrom("ehcache.xml", ehCacheConfigurationListener);
        shutdown();
    }

    @Override
    public Datastore getDefaultDatastore() {
        return localDatastoreManager.getDatastore();
    }

    @Override
    public Datastore getDatastore(String datastoreName) {
        if (distributedManagers.containsKey(datastoreName)) {
            return distributedManagers.get(datastoreName).getDatastore();
        }

        return null;
    }

    @Override
    public DistributedDatastore getDistributedDatastore() {
        if (!distributedManagers.isEmpty()) {
            return (DistributedDatastore) distributedManagers.values().iterator().next().getDatastore();
        }
        return null;
    }

    @Override
    public void destroyDatastore(String datastoreName) {
        final DatastoreManager managerToUnregister = distributedManagers.get(datastoreName);

        if (managerToUnregister != null) {
            distributedManagers.remove(datastoreName);
            try {
                managerToUnregister.destroy();
            } catch (Exception e) {
                LOG.warn("Failed to shutdown datastore " + datastoreName + " with exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public DistributedDatastore createDatastore(String datastoreName, ClusterConfiguration configuration) {
        DatastoreManager manager = new HashRingDatastoreManager(configuration, localDatastoreManager.getDatastore());
        distributedManagers.put(datastoreName, manager);
        return (DistributedDatastore) manager.getDatastore();
    }

    @Override
    public void shutdown() {
        for (String datastoreName : distributedManagers.keySet()) {
            destroyDatastore(datastoreName);
        }
        localDatastoreManager.destroy();
    }

    private class EHCacheConfigurationListener implements UpdateListener<ConfigurationResource> {
        private boolean initialized = false;

        @Override
        public synchronized void configurationUpdated(ConfigurationResource config) throws UpdateFailedException {
            try {
                EHCacheDatastoreManager newDatastoreManager = new EHCacheDatastoreManager(config.newInputStream());
                localDatastoreManager.destroy();
                localDatastoreManager = newDatastoreManager;
            } catch (Exception e) {
                LOG.error("Configuration could not be read, continuing to use current datastore");
            }

            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
