package com.rackspace.papi.service.httpconnectionpool;

import org.apache.http.client.HttpClient;

import java.util.Set;

public interface HttpConnectionPoolService {

    public HttpClient getDefaultClient();

    public HttpClient getClient(String id);

    public Set<String> getAvailablePools();
}
