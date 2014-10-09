package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.services.datastore.impl.distributed.DatastoreHeader;
import org.openrepose.services.datastore.impl.distributed.CacheRequest;
import org.openrepose.services.datastore.api.distributed.RemoteBehavior;
import org.openrepose.services.datastore.impl.distributed.remote.RemoteCommand;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

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
