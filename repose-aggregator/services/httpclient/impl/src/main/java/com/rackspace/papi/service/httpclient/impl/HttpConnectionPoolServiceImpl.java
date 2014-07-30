package com.rackspace.papi.service.httpclient.impl;

import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpclient.config.PoolType;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.config.manager.UpdateListener;
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

import static com.rackspace.papi.service.httpclient.impl.HttpConnectionPoolProvider.CLIENT_INSTANCE_ID;


@Named
public class HttpConnectionPoolServiceImpl implements HttpClientService<HttpConnectionPoolConfig, HttpClientResponseImpl> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpConnectionPoolServiceImpl.class);
    public static final String DEFAULT_CONFIG_NAME = "http-connection-pool.cfg.xml";
    private static final String HTTP_CONNECTION_POOL_SERVICE_REPORT = "HttpConnectionPoolServiceReport";

    private static PoolType DEFAULT_POOL;
    private Map<String, HttpClient> poolMap;
    private String defaultClientId;
    private ClientDecommissionManager decommissionManager;
    private HttpClientUserManager httpClientUserManager;
    private ConfigurationService configurationService;
    private ConfigurationListener configurationListener;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Inject
    public HttpConnectionPoolServiceImpl(ConfigurationService configurationService, HealthCheckService healthCheckService) {
        LOG.debug("Creating New HTTP Connection Pool Service");

        poolMap = new HashMap<String, HttpClient>();
        DEFAULT_POOL = new PoolType();
        httpClientUserManager = new HttpClientUserManager();
        decommissionManager = new ClientDecommissionManager(httpClientUserManager);
        decommissionManager.startThread();
        configurationListener = new ConfigurationListener();
        healthCheckServiceProxy = healthCheckService.register();
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        LOG.debug("Initializing context for HTTPConnectionPool");
        reportIssue();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-connection-pool.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, HttpConnectionPoolConfig.class);

        // The Http Connection Pool config is optional so in the case where the configuration listener doesn't mark it iniitalized
        // and the file doesn't exist, this means that the Http Connection Pool service will load its own default configuration
        // and the initial health check error should be cleared.
        try {
            if (!configurationListener.isInitialized() && !configurationService.getResourceResolver().resolve(DEFAULT_CONFIG_NAME).exists()) {
                solveIssue();
            }
        } catch (IOException io) {
            LOG.error("Error attempting to search for {}", DEFAULT_CONFIG_NAME, io);
        }
    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        LOG.debug("Destroying context for HTTPConnectionPool");
        shutdown();
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, configurationListener);
    }

    @Override
    public HttpClientResponse getClient(String clientId) throws HttpClientNotFoundException {

        if (poolMap.isEmpty()) {
            defaultClientId = "DEFAULT_POOL";
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
    public void releaseClient(HttpClientResponseImpl httpClientResponse) {
        String clientInstanceId = httpClientResponse.getClientInstanceId();
        String userId = httpClientResponse.getUserId();

        httpClientUserManager.removeUser(clientInstanceId, userId);
    }

    @Override
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
        } else {
            return DEFAULT_POOL.getHttpConnManagerMaxTotal();
        }
    }

    private HttpClient clientGenerator(PoolType poolType) {
        return HttpConnectionPoolProvider.genClient(poolType);
    }

    private void reportIssue() {
        LOG.debug("Reporting issue to Health Checker Service: " + HTTP_CONNECTION_POOL_SERVICE_REPORT);
        healthCheckServiceProxy.reportIssue(HTTP_CONNECTION_POOL_SERVICE_REPORT, "Metrics Service Configuration Error", Severity.BROKEN);
    }

    private void solveIssue() {
        LOG.debug("Resolving issue: " + HTTP_CONNECTION_POOL_SERVICE_REPORT);
        healthCheckServiceProxy.resolveIssue(HTTP_CONNECTION_POOL_SERVICE_REPORT);
    }


    private class ConfigurationListener implements UpdateListener<HttpConnectionPoolConfig> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(HttpConnectionPoolConfig poolConfig) {
            configure(poolConfig);
            initialized = true;
            solveIssue();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
