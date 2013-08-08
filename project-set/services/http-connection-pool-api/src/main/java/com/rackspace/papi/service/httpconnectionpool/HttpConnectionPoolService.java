package com.rackspace.papi.service.httpconnectionpool;

import org.apache.http.client.HttpClient;
import java.util.Set;

/**
 * HttpConnectionPoolService - proxies HTTP requests using a configurable pool of
 * connections
 *
 */
public interface HttpConnectionPoolService<I> {

    /**
     *
     * @param id
     * @return an HttpClient
     */
    public HttpClient getClient(String id);

    public void configure(I config);

    public boolean isAvailable(String poolName);

    public Set<String> getAvailablePools();
}
