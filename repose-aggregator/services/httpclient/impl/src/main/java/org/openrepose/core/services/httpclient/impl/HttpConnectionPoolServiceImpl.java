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

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.pool.PoolStats;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig;
import org.openrepose.core.service.httpclient.config.PoolType;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.openrepose.core.services.httpclient.HttpClientNotFoundException;
import org.openrepose.core.services.httpclient.HttpClientResponse;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.openrepose.core.services.httpclient.impl.HttpConnectionPoolProvider.CLIENT_INSTANCE_ID;


@Named
public class HttpConnectionPoolServiceImpl implements HttpClientService {

    public static final String DEFAULT_CONFIG_NAME = "http-connection-pool.cfg.xml";
    private static final String DEFAULT_POOL_ID = "DEFAULT_POOL";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpConnectionPoolServiceImpl.class);
    private static final String httpConnectionPoolServiceReport = "HttpConnectionPoolServiceReport";
    private static PoolType DEFAULT_POOL = new PoolType();
    private final ConfigurationService configurationService;
    private final HttpClientUserManager httpClientUserManager;
    private final HealthCheckServiceProxy healthCheckServiceProxy;
    private final ConfigurationListener configurationListener;
    private Map<String, HttpClient> poolMap;
    private String defaultClientId;
    private ClientDecommissionManager decommissionManager;

    @Inject
    public HttpConnectionPoolServiceImpl(
            ConfigurationService configurationService,
            HealthCheckService healthCheckService
    ) {
        this.configurationService = configurationService;
        this.healthCheckServiceProxy = healthCheckService.register();
        LOG.debug("Creating New HTTP Connection Pool Service");
        poolMap = new HashMap<>();
        httpClientUserManager = new HttpClientUserManager();

        configurationListener = new ConfigurationListener();
        decommissionManager = new ClientDecommissionManager(httpClientUserManager);

    }

    @PostConstruct
    public void init() {
        LOG.debug("Initializing HttpConnectionPoolService");
        decommissionManager.startThread();

        //Set up the configuration listener

        healthCheckServiceProxy.reportIssue(httpConnectionPoolServiceReport, "Http Client Service Configuration Error", Severity.BROKEN);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-connection-pool.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, HttpConnectionPoolConfig.class);

        // The Http Connection Pool config is optional so in the case where the configuration listener doesn't mark it iniitalized
        // and the file doesn't exist, this means that the Http Connection Pool service will load its own default configuration
        // and the initial health check error should be cleared.
        try {
            if (!configurationListener.isInitialized() && !configurationService.getResourceResolver().resolve(DEFAULT_CONFIG_NAME).exists()) {
                healthCheckServiceProxy.resolveIssue(httpConnectionPoolServiceReport);
            }
        } catch (IOException io) {
            LOG.error("Error attempting to search for {}", DEFAULT_CONFIG_NAME, io);
        }
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, configurationListener);
        shutdown();
    }

    @Override
    public HttpClientResponse getClient(String clientId) throws HttpClientNotFoundException {

        if (poolMap.isEmpty()) {
            defaultClientId = DEFAULT_POOL_ID;
            HttpClient httpClient = clientGenerator(DEFAULT_POOL);
            poolMap.put(defaultClientId, httpClient);
        }

        if (clientId != null && !clientId.isEmpty() && !isAvailable(clientId)) {
            HttpClient httpClient = clientGenerator(DEFAULT_POOL);
            poolMap.put(clientId, httpClient);
        }

        final HttpClient requestedClient;

        if (clientId == null || clientId.isEmpty()) {
            requestedClient = poolMap.get(defaultClientId);
        } else {
            if (isAvailable(clientId)) {
                requestedClient = poolMap.get(clientId);
            } else {
                throw new HttpClientNotFoundException("Pool " + clientId + "not available");
            }
        }

        String clientInstanceId = requestedClient.getParams().getParameter(CLIENT_INSTANCE_ID).toString();
        String userId = httpClientUserManager.addUser(clientInstanceId);

        PoolStats poolStats = ((PoolingClientConnectionManager) requestedClient.getConnectionManager()).getTotalStats();
        LOG.trace("Client requested, pool currently leased: {}, available: {}, pending: {}, max: {}", poolStats.getLeased(), poolStats.getAvailable(), poolStats.getPending(), poolStats.getMax());

        return new HttpClientResponseImpl(requestedClient, clientId, clientInstanceId, userId);
    }

    @Override
    public void releaseClient(HttpClientResponse httpClientResponse) {
        String clientInstanceId = httpClientResponse.getClientInstanceId();
        String userId = httpClientResponse.getUserId();

        httpClientUserManager.removeUser(clientInstanceId, userId);
    }

    public void configure(HttpConnectionPoolConfig config) {
        HashMap<String, HttpClient> newPoolMap = new HashMap<String, HttpClient>();

        for (PoolType poolType : config.getPool()) {
            if (poolType.isDefault()) {
                defaultClientId = poolType.getId();
            }
            newPoolMap.put(poolType.getId(), clientGenerator(poolType));
        }

        if (!poolMap.isEmpty()) {
            decommissionManager.decommissionClient(poolMap);
        }

        poolMap = newPoolMap;
    }

    @Override
    public boolean isAvailable(String clientId) {
        return poolMap.containsKey(clientId);
    }

    @Override
    public Set<String> getAvailableClients() {
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

    @Override
    public int getMaxConnections(String clientId) {
        if (poolMap.containsKey(clientId)) {
            return ((PoolingClientConnectionManager) poolMap.get(clientId).getConnectionManager()).getMaxTotal();
        } else if (poolMap.containsKey(defaultClientId)) {
            return ((PoolingClientConnectionManager) poolMap.get(defaultClientId).getConnectionManager()).getMaxTotal();
        } else {
            return DEFAULT_POOL.getHttpConnManagerMaxTotal();
        }
    }

    @Override
    public int getSocketTimeout(String clientId) {
        if (poolMap.containsKey(clientId)) {
            return poolMap.get(clientId).getParams().getIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0);
        } else if (poolMap.containsKey(defaultClientId)) {
            return poolMap.get(defaultClientId).getParams().getIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0);
        } else {
            return DEFAULT_POOL.getHttpSocketTimeout();
        }
    }

    private HttpClient clientGenerator(PoolType poolType) {
        return HttpConnectionPoolProvider.genClient(poolType);
    }

    private class ConfigurationListener implements UpdateListener<HttpConnectionPoolConfig> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(HttpConnectionPoolConfig poolConfig) {
            configure(poolConfig);
            initialized = true;
            healthCheckServiceProxy.resolveIssue(httpConnectionPoolServiceReport);
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

}
