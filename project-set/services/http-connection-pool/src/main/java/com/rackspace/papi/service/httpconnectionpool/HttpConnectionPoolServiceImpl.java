package com.rackspace.papi.service.httpconnectionpool;

import com.rackspace.papi.service.httpconnectionpool.config.HttpConnectionPoolConfig;
import com.rackspace.papi.service.httpconnectionpool.config.PoolType;
import org.apache.http.client.HttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpConnectionPoolServiceImpl implements HttpConnectionPoolService {

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
    public HttpClient getDefaultClient() {
        return poolMap.get(defaultClientId);
    }

    @Override
    public HttpClient getClient(String id) {
        return poolMap.get(id);
    }

    @Override
    public Set<String> getAvailablePools() {
        return poolMap.keySet();
    }
}
