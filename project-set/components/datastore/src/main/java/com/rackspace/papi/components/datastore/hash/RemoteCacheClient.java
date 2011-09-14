/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.StoredElement;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zinic
 */
public interface RemoteCacheClient {

    StoredElement get(String key, InetSocketAddress remoteEndpoint);

    void put(String key, byte[] value, int ttl, TimeUnit timeUnit, InetSocketAddress remoteEndpoint);
    
}
