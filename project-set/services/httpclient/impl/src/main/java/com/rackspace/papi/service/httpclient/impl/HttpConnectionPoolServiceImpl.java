package com.rackspace.papi.service.httpclient.impl;

import com.rackspace.papi.service.httpclient.DefaultHttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpclient.config.PoolType;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class HttpConnectionPoolServiceImpl implements HttpClientService<HttpConnectionPoolConfig> {

    Map<String, HttpClient> poolMap;
    String defaultClientId;
    private static PoolType DEFAULT_POOL;
    ClientDecommissionManager decommissionManager;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpConnectionPoolServiceImpl.class);

    public HttpConnectionPoolServiceImpl() {
        LOG.debug("Creating New HTTP Connection Pool Service");

        poolMap = new HashMap<String, HttpClient>();
        DEFAULT_POOL = new PoolType();
        decommissionManager = new ClientDecommissionManager();
        decommissionManager.startThread();


    }

    @Override
    public HttpClientResponse getClient(String clientId) throws HttpClientNotFoundException {


        if (poolMap.isEmpty() ) {
            defaultClientId = "DEFAULT_POOL";
            HttpClient httpClient = HttpConnectionPoolProvider.genClient(DEFAULT_POOL);
            poolMap.put(defaultClientId, httpClient);

        }

        if(clientId != null && !clientId.isEmpty() && !isAvailable(clientId)) {

            HttpClient httpClient = HttpConnectionPoolProvider.genClient(DEFAULT_POOL);
            poolMap.put(clientId, httpClient);
        }

        if (clientId == null || clientId.isEmpty()) {
            return new DefaultHttpClientResponse(poolMap.get(defaultClientId), clientId);
        } else {
            if (isAvailable(clientId))
                return new DefaultHttpClientResponse(poolMap.get(clientId), clientId);
            else
                throw new HttpClientNotFoundException("Pool " + clientId + "not available");
        }
    }

    @Override
    public void configure(HttpConnectionPoolConfig config) {


        HashMap<String, HttpClient> newPoolMap = new HashMap<String, HttpClient>();

        for (PoolType poolType : config.getPool()) {
            if (poolType.isDefault()) {
                defaultClientId = poolType.getId();
            }
            newPoolMap.put(poolType.getId(), HttpConnectionPoolProvider.genClient(poolType));
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
}
