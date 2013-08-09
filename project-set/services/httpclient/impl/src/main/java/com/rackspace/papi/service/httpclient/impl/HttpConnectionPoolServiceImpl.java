package com.rackspace.papi.service.httpclient.impl;

import com.rackspace.papi.service.httpclient.DefaultHttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.rackspace.papi.service.httpclient.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpclient.config.PoolType;
import org.apache.http.client.HttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpConnectionPoolServiceImpl implements HttpClientService<HttpConnectionPoolConfig> {

    Map<String, HttpClient> poolMap;
    String defaultClientId;

    public HttpConnectionPoolServiceImpl(HttpConnectionPoolConfig conf) {

        poolMap = new HashMap<String, HttpClient>();

        for (PoolType poolType : conf.getPool()) {
            if (poolType.isDefault()) {
                defaultClientId = poolType.getId();
            }
            poolMap.put(poolType.getId(), HttpConnectionPoolProvider.genClient(poolType));
        }
    }

    @Override
    public HttpClientResponse getClient(String clientId) throws HttpClientNotFoundException {
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
        poolMap = new HashMap<String, HttpClient>();

        for (PoolType poolType : config.getPool()) {
            if (poolType.isDefault()) {
                defaultClientId = poolType.getId();
            }
            poolMap.put(poolType.getId(), HttpConnectionPoolProvider.genClient(poolType));
        }

        //TODO: Decommission logic
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
        throw new UnsupportedOperationException("implement me");
    }
}
