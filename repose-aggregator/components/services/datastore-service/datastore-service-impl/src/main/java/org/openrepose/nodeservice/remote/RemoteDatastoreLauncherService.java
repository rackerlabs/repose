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
package org.openrepose.nodeservice.remote;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.remote.config.RemoteClusterConfiguration;
import org.openrepose.core.services.datastore.remote.config.RemoteDatastoreConfiguration;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.config.ReposeCluster;
import org.openrepose.core.systemmodel.config.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Named
public class RemoteDatastoreLauncherService {
    public static final String DEFAULT_CONFIG_NAME = "remote-datastore.cfg.xml";
    public static final String DEFAULT_CONFIG_SCHEMA = "/META-INF/schema/remote-datastore/remote-datastore.xsd";
    public static final String REMOTE_DATASTORE_NAME = "remote-datastore";
    public static final String SYSTEM_CONFIG_NAME = "system-model.cfg.xml";
    public static final String SYSTEM_CONFIG_SCHEMA = "/META-INF/schema/system-model/system-model.xsd";

    private static final Logger LOG = LoggerFactory.getLogger(RemoteDatastoreLauncherService.class);
    private static final String REMOTE_DATASTORE_SERVICE_CONFIG_ISSUE = "remote-datastore-config-issue";

    private final String clusterId;
    private final String nodeId;
    private final DatastoreService datastoreService;
    private final HealthCheckServiceProxy healthCheckServiceProxy;
    private final RequestProxyService requestProxyService;
    private final ConfigurationService configurationService;
    private final SystemModelListener systemModelListener;
    private final RemoteDatastoreConfigurationListener configurationListener;
    private final AtomicReference<RemoteDatastoreConfiguration> currentConfiguration = new AtomicReference<>();
    private volatile boolean isRunning = false;

    @Inject
    public RemoteDatastoreLauncherService(
            @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
            @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId,
            DatastoreService datastoreService,
            ConfigurationService configurationService,
            HealthCheckService healthCheckService,
            RequestProxyService requestProxyService) {

        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.datastoreService = datastoreService;
        this.healthCheckServiceProxy = healthCheckService.register();
        this.requestProxyService = requestProxyService;
        this.configurationService = configurationService;
        this.systemModelListener = new SystemModelListener();
        this.configurationListener = new RemoteDatastoreConfigurationListener();
    }

    @PostConstruct
    public void init() {
        //Subscribe to the system model to know if we even want to turn on...
        //If and only if we're going to be turned on, should we subscribe to the other one
        URL systemModelXSD = getClass().getResource(SYSTEM_CONFIG_SCHEMA);
        configurationService.subscribeTo(SYSTEM_CONFIG_NAME, systemModelXSD, systemModelListener, SystemModel.class);
    }

    private void initRemoteDatastore() {
        healthCheckServiceProxy.reportIssue(REMOTE_DATASTORE_SERVICE_CONFIG_ISSUE, "Remote Datastore Service Configuration Error", Severity.BROKEN);
        URL xsdURL = getClass().getResource(DEFAULT_CONFIG_SCHEMA);
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, RemoteDatastoreConfiguration.class);
    }

    private void destroyRemoteDatastore() {
        isRunning = false;
        datastoreService.destroyDatastore(REMOTE_DATASTORE_NAME);
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, configurationListener);
        healthCheckServiceProxy.resolveIssue(REMOTE_DATASTORE_SERVICE_CONFIG_ISSUE);
    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        configurationService.unsubscribeFrom(SYSTEM_CONFIG_NAME, systemModelListener);
        destroyRemoteDatastore();
    }

    private class SystemModelListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            SystemModelInterrogator smi = new SystemModelInterrogator(clusterId, nodeId);
            Optional<ReposeCluster> clusterOption = smi.getLocalCluster(configurationObject);
            if (clusterOption.isPresent()) {
                boolean listed = smi.getServiceForCluster(configurationObject, REMOTE_DATASTORE_NAME).isPresent();

                if (listed && !isRunning) {
                    //Note it as being broke, until it's properly configured.
                    healthCheckServiceProxy.reportIssue(
                            REMOTE_DATASTORE_SERVICE_CONFIG_ISSUE,
                            "Remote Datastore Configuration Issue: Remote Datastore Specified in system model, but not yet configured",
                            Severity.BROKEN);
                    initRemoteDatastore();
                } else if (!listed && isRunning) {
                    //any health check problems are resolved when we stop it
                    destroyRemoteDatastore();
                } else if (!listed) {
                    //Resolve the health check issue -- the user no longer wants to use this service, so
                    //it does not matter that this service is not in a working state
                    healthCheckServiceProxy.resolveIssue(REMOTE_DATASTORE_SERVICE_CONFIG_ISSUE);
                }
            }
        }

        @Override
        public boolean isInitialized() {
            return isRunning;
        }
    }

    private class RemoteDatastoreConfigurationListener implements UpdateListener<RemoteDatastoreConfiguration> {
        private boolean initialized = false;

        @Override
        public void configurationUpdated(RemoteDatastoreConfiguration configurationObject) {
            RemoteDatastoreConfiguration currentRemoteDatastoreConfiguration = currentConfiguration.get();
            Optional<RemoteClusterConfiguration> oldClusterConfigOpt = Optional.ofNullable(currentRemoteDatastoreConfiguration)
                    .flatMap(s -> s.getCluster().stream()
                            .filter(cluster -> cluster.getId().equals(clusterId))
                            .findFirst());
            Optional<RemoteClusterConfiguration> newClusterConfigOpt = configurationObject
                    .getCluster()
                    .stream()
                    .filter(cluster -> cluster.getId().equals(clusterId))
                    .findFirst();

            // IF only the old config had a Cluster for this ID,
            // THEN destroy it;
            // ELSE IF only the new config has a Cluster for this ID,
            // THEN simple initialize it;
            // ELSE IF both the old and new configs had a Cluster for this ID,
            // AND something has changed,
            // THEN destroy the old one
            // AND initialize the new one;
            // ELSE nothing has changed for this Cluster ID,
            // SO do nothing.
            if (oldClusterConfigOpt.isPresent() && !newClusterConfigOpt.isPresent()) {
                destroyRemoteDatastore();
            } else if (!oldClusterConfigOpt.isPresent() && newClusterConfigOpt.isPresent()) {
                RemoteClusterConfiguration newClusterConfig = newClusterConfigOpt.get();
                datastoreService.createRemoteDatastore(
                        REMOTE_DATASTORE_NAME,
                        requestProxyService,
                        UUIDEncodingProvider.getInstance(),
                        new InetSocketAddress(newClusterConfig.getHost(), newClusterConfig.getPort()),
                        newClusterConfig.getConnectionPoolId(),
                        newClusterConfig.isUseHTTPS());
            } else if (oldClusterConfigOpt.isPresent() /*&& newClusterConfigOpt.isPresent()*/) {
                RemoteClusterConfiguration oldClusterConfig = oldClusterConfigOpt.get();
                RemoteClusterConfiguration newClusterConfig = newClusterConfigOpt.get();
                if (!oldClusterConfig.getHost().equalsIgnoreCase(newClusterConfig.getHost()) ||
                        !oldClusterConfig.getConnectionPoolId().equals(newClusterConfig.getConnectionPoolId()) ||
                        oldClusterConfig.getPort() != newClusterConfig.getPort() ||
                        oldClusterConfig.isUseHTTPS() != newClusterConfig.isUseHTTPS()) {
                    destroyRemoteDatastore();
                    datastoreService.createRemoteDatastore(
                            REMOTE_DATASTORE_NAME,
                            requestProxyService,
                            UUIDEncodingProvider.getInstance(),
                            new InetSocketAddress(newClusterConfig.getHost(), newClusterConfig.getPort()),
                            newClusterConfig.getConnectionPoolId(),
                            newClusterConfig.isUseHTTPS());
                }
            } //else { /* DO NOTHING */ }
            currentConfiguration.set(configurationObject);
            initialized = true;
            healthCheckServiceProxy.resolveIssue(REMOTE_DATASTORE_SERVICE_CONFIG_ISSUE);
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
