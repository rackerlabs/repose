package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.StoredElement;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public interface RemoteCacheClient {

    StoredElement get(String key, InetSocketAddress remoteEndpoint) throws RemoteConnectionException;

    void put(String key, byte[] value, int ttl, TimeUnit timeUnit, InetSocketAddress remoteEndpoint) throws RemoteConnectionException;
}
