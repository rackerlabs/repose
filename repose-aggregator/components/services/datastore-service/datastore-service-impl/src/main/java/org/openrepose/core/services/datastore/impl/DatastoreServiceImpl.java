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

import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreManager;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.datastore.impl.distributed.HashRingDatastoreManager;
import org.openrepose.core.services.datastore.impl.ehcache.EHCacheDatastoreManager;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Named
public class DatastoreServiceImpl implements DatastoreService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DatastoreServiceImpl.class);

    private final DatastoreManager localDatastoreManager;
    private final Map<String, DatastoreManager> distributedManagers;

    @Inject
    public DatastoreServiceImpl(Optional<MetricsService> metricsService) {
        localDatastoreManager = new EHCacheDatastoreManager(metricsService);
        distributedManagers = new HashMap<>();
    }

    @PreDestroy
    public void destroy() {
        LOG.info("Destroying datastore service context");
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
        return createDistributedDatastore(datastoreName, configuration, null, false);
    }

    @Override
    public DistributedDatastore createDistributedDatastore(String datastoreName, ClusterConfiguration configuration, String connPoolId, boolean useHttps) {
        DatastoreManager manager = new HashRingDatastoreManager(configuration, localDatastoreManager.getDatastore(), connPoolId, useHttps);
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
}
