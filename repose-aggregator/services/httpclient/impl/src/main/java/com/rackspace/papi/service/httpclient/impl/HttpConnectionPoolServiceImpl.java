package com.rackspace.papi.service.httpclient.impl;

import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpclient.config.PoolType;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.rackspace.papi.service.httpclient.impl.HttpConnectionPoolProvider.CLIENT_INSTANCE_ID;


public class HttpConnectionPoolServiceImpl implements HttpClientService<HttpConnectionPoolConfig, HttpClientResponseImpl> {

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
        httpClientUserManager = new HttpClientUserManager();
        decommissionManager = new ClientDecommissionManager(httpClientUserManager);
        decommissionManager.startThread();
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
}
