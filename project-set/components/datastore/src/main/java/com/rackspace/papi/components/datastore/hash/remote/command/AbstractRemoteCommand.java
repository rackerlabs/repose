package com.rackspace.papi.components.datastore.hash.remote.command;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.components.datastore.common.CacheRequest;
import com.rackspace.papi.components.datastore.common.DatastoreHeader;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.components.datastore.hash.remote.RemoteCommand;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zinic
 */
public abstract class AbstractRemoteCommand implements RemoteCommand {

    private final InetSocketAddress remoteEndpoint;
    private final String cacheObjectKey;
    private String hostKey;

    public AbstractRemoteCommand(String cacheObjectKey, InetSocketAddress remoteEndpoint) {
        this.cacheObjectKey = cacheObjectKey;
        this.remoteEndpoint = remoteEndpoint;
    }
    
    public String getUrl() {
        return CacheRequest.urlFor(getRemoteEndpoint(), getCacheObjectKey());
    }
    
    public String getBaseUrl() {
        return CacheRequest.urlFor(remoteEndpoint);
    }
    
    @Override
    public abstract ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior);

    protected byte[] getBody() {
        return null;
    }

    protected Map<String, String> getHeaders(RemoteBehavior remoteBehavior) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(DatastoreHeader.HOST_KEY.toString(), hostKey);
        headers.put(DatastoreHeader.REMOTE_BEHAVIOR.toString(), remoteBehavior.name());
        
        return headers;
    }

    protected InetSocketAddress getRemoteEndpoint() {
        return remoteEndpoint;
    }

    protected String getCacheObjectKey() {
        return cacheObjectKey;
    }

    @Override
    public void setHostKey(String hostKey) {
        this.hostKey = hostKey;
    }
}
