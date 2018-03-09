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
package org.openrepose.core.services.httpclient.impl;

import io.opentracing.Tracer;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig;
import org.openrepose.core.service.httpclient.config.PoolType;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.openrepose.core.services.httpclient.HttpClientContainer;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.openrepose.core.services.httpclient.impl.HttpConnectionPoolProvider.CLIENT_INSTANCE_ID;


@Named
public class HttpConnectionPoolServiceImpl implements HttpClientService {

    public static final String DEFAULT_CONFIG_NAME = "http-connection-pool.cfg.xml";

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpConnectionPoolServiceImpl.class);
    private static final String HTTP_CONNECTION_POOL_SERVICE_REPORT = "HttpConnectionPoolServiceReport";

    private final ConfigurationService configurationService;
    private final HttpClientUserManager httpClientUserManager;
    private final HealthCheckServiceProxy healthCheckServiceProxy;
    private final ConfigurationListener configurationListener;
    private final Tracer tracer;
    private final String configRoot;

    private boolean initialized = false;
    private String defaultClientId;
    private ClientDecommissionManager decommissionManager;
    private Map<String, HttpClient> poolMap;

    @Inject
    public HttpConnectionPoolServiceImpl(
            ConfigurationService configurationService,
            HealthCheckService healthCheckService,
            Tracer tracer,
            @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) String configRoot
    ) {
        LOG.debug("Creating New HTTP Connection Pool Service");

        this.configurationService = configurationService;
        this.tracer = tracer;
        this.healthCheckServiceProxy = healthCheckService.register();
        this.configRoot = configRoot;
        this.poolMap = new HashMap<>();
        this.httpClientUserManager = new HttpClientUserManager();
        this.configurationListener = new ConfigurationListener();
        this.decommissionManager = new ClientDecommissionManager(httpClientUserManager);
    }

    @PostConstruct
    public void init() {
        LOG.debug("Initializing HttpConnectionPoolService");

        decommissionManager.startThread();
        healthCheckServiceProxy.reportIssue(HTTP_CONNECTION_POOL_SERVICE_REPORT, "Http Client Service Configuration Error", Severity.BROKEN);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-connection-pool.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, HttpConnectionPoolConfig.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, configurationListener);
        shutdown();
    }

    @Override
    public HttpClientContainer getDefaultClient() {
        return getClient(null);
    }

    @Override
    public HttpClientContainer getClient(String clientId) {
        verifyInitialized();

        final HttpClient requestedClient;

        if (clientId == null) {
            requestedClient = poolMap.get(defaultClientId);
        } else if (!isAvailable(clientId)) {
            LOG.warn("Pool " + clientId + " not available -- returning the default pool");
            requestedClient = poolMap.get(defaultClientId);
        } else {
            requestedClient = poolMap.get(clientId);
        }

        String clientInstanceId = requestedClient.getParams().getParameter(CLIENT_INSTANCE_ID).toString();
        String userId = httpClientUserManager.addUser(clientInstanceId);

        PoolStats poolStats = ((PoolingClientConnectionManager) requestedClient.getConnectionManager()).getTotalStats();
        LOG.trace("Client requested, pool currently leased: {}, available: {}, pending: {}, max: {}", poolStats.getLeased(), poolStats.getAvailable(), poolStats.getPending(), poolStats.getMax());

        return new HttpClientContainerImpl(requestedClient,
                clientInstanceId,
                userId);
    }

    @Override
    public void releaseClient(HttpClientContainer httpClientContainer) {
        verifyInitialized();

        String clientInstanceId = httpClientContainer.getClientInstanceId();
        String userId = httpClientContainer.getUserId();

        httpClientUserManager.removeUser(clientInstanceId, userId);
    }

    @Override
    public boolean isAvailable(String clientId) {
        verifyInitialized();

        return poolMap.containsKey(clientId);
    }

    @Override
    public Set<String> getAvailableClients() {
        verifyInitialized();

        return poolMap.keySet();
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down HTTP connection pools");
        for (HttpClient client : poolMap.values()) {
            client.getConnectionManager().shutdown();
        }
        decommissionManager.stopThread();
    }

    public void configure(HttpConnectionPoolConfig config) {
        HashMap<String, HttpClient> newPoolMap = new HashMap<>();

        for (PoolType poolType : config.getPool()) {
            if (poolType.isDefault()) {
                defaultClientId = poolType.getId();
            }
            newPoolMap.put(poolType.getId(), clientGenerator(configRoot, poolType));
        }

        if (!poolMap.isEmpty()) {
            decommissionManager.decommissionClient(poolMap);
        }

        poolMap = newPoolMap;
    }

    private void verifyInitialized() {
        if (!initialized) {
            throw new IllegalStateException("The HttpConnectionPoolService has not yet been initialized");
        }
    }

    private HttpClient clientGenerator(String configRoot, PoolType poolType) {
        return HttpConnectionPoolProvider.genClient(configRoot, poolType, tracer);
    }

    private class ConfigurationListener implements UpdateListener<HttpConnectionPoolConfig> {
        @Override
        public void configurationUpdated(HttpConnectionPoolConfig poolConfig) {
            configure(poolConfig);
            healthCheckServiceProxy.resolveIssue(HTTP_CONNECTION_POOL_SERVICE_REPORT);
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
