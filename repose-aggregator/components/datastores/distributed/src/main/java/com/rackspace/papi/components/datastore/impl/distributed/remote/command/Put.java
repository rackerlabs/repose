package com.rackspace.papi.components.datastore.impl.distributed.remote.command;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Put extends AbstractRemoteCommand {

    private final TimeUnit timeUnit;
    private final Serializable value;
    private final int ttl;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public Put(TimeUnit timeUnit, Serializable value, int ttl, String cacheObjectKey, InetSocketAddress remoteEndpoint) {
        super(cacheObjectKey, remoteEndpoint);
        this.timeUnit = timeUnit;
        this.ttl = ttl;
        this.value = value;
    }

    @Override
    protected Map<String, String> getHeaders(RemoteBehavior remoteBehavior) {
        Map<String, String> headers = super.getHeaders(remoteBehavior);
        headers.put(ExtendedHttpHeader.X_TTL.toString(), String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));
        return headers;
    }
    
    @Override
    protected byte[] getBody() {
        try {
            return ObjectSerializer.instance().writeObject(value);
        } catch (IOException ioe) {
            throw new DatastoreOperationException("Failed to serialize value to be put", ioe);
        }
    }
    
    @Override
    public Object handleResponse(ServiceClientResponse response) throws IOException {
        if (response.getStatusCode() != HttpStatusCode.ACCEPTED.intValue()) {
            throw new DatastoreOperationException("Remote request failed with: " + response.getStatusCode());
        }

        return Boolean.TRUE;
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.put(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior), getBody());
    }

}
