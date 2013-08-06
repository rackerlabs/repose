package com.rackspace.repose.service.httpconnectionpool;


import org.apache.http.client.HttpClient;

import java.util.Set;

/**
 * HttpConnectionPoolService - proxies HTTP requests using a configurable pool of
 * connections
 *
 */
public interface HttpConnectionPoolService {

    public HttpClient getDefaultClient();

    public HttpClient getClient(String id);

    public Set<String> getAvailablePools();

}
