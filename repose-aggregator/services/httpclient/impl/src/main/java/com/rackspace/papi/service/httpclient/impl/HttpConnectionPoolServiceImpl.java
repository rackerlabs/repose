package com.rackspace.papi.service.httpclient.impl;

import com.rackspace.papi.service.httpclient.DefaultHttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpclient.config.PoolType;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class HttpConnectionPoolServiceImpl implements HttpClientService<HttpConnectionPoolConfig, DefaultHttpClientResponse> {

    private static PoolType DEFAULT_POOL;
    private Map<String, HttpClient> poolMap;
    private String defaultClientId;
    private ClientDecommissionManager decommissionManager;
    private HttpClientUserManager httpClientUserManager;

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpConnectionPoolServiceImpl.class);

    public HttpConnectionPoolServiceImpl() {
        LOG.debug("Creating New HTTP Connection Pool Service");

        poolMap = new HashMap<String, HttpClient>();
        DEFAULT_POOL = new PoolType();
        decommissionManager = new ClientDecommissionManager();
        decommissionManager.startThread();
        httpClientUserManager = new HttpClientUserManager();
    }

    @Override
    public DefaultHttpClientResponse getClient(String clientId) throws HttpClientNotFoundException {

        if (poolMap.isEmpty()) {
            defaultClientId = "DEFAULT_POOL";
            HttpClient httpClient = clientGenerator(DEFAULT_POOL);
            poolMap.put(defaultClientId, httpClient);
        }

        if (clientId != null && !clientId.isEmpty() && !isAvailable(clientId)) {
            HttpClient httpClient = clientGenerator(DEFAULT_POOL);
            poolMap.put(clientId, httpClient);
        }

        if (clientId == null || clientId.isEmpty()) {
            String userId = httpClientUserManager.addUser(defaultClientId);
            return new DefaultHttpClientResponse(poolMap.get(defaultClientId), clientId, userId);
        } else {
            if (isAvailable(clientId)) {
                String userId = httpClientUserManager.addUser(clientId);
                return new DefaultHttpClientResponse(poolMap.get(clientId), clientId, userId);
            } else {
                throw new HttpClientNotFoundException("Pool " + clientId + "not available");
            }
        }
    }

    @Override
    public void releaseClient(DefaultHttpClientResponse httpClientResponse) {
        String clientId = httpClientResponse.getClientId();
        String userId = httpClientResponse.getUserId();

        httpClientUserManager.removeUser(clientId, userId);
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
    public int getPoolSize(String poolID) {

        if (poolMap.containsKey(poolID)) {
            return ((PoolingClientConnectionManager) poolMap.get(poolID).getConnectionManager()).getMaxTotal();
        } else {
            return DEFAULT_POOL.getHttpConnManagerMaxTotal();
        }
    }

    private HttpClient clientGenerator(PoolType poolType) {
        final HttpClient httpClient = HttpConnectionPoolProvider.genClient(poolType);
        return httpClient;
    }
}
