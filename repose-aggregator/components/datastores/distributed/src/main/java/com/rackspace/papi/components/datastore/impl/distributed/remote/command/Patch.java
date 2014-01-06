package com.rackspace.papi.components.datastore.impl.distributed.remote.command;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * User: dimi5963
 * Date: 1/4/14
 * Time: 3:36 PM
 * TODO: update with Jorge's suggestions
 */
public class Patch extends AbstractRemoteCommand {

    private final TimeUnit timeUnit;
    private final byte[] value;
    private final int ttl;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public Patch(TimeUnit timeUnit, byte[] value, int ttl, String cacheObjectKey, InetSocketAddress remoteEndpoint) {
        super(cacheObjectKey, remoteEndpoint);
        this.timeUnit = timeUnit;
        this.value = value;
        this.ttl = ttl;
    }

    @Override
    protected Map<String, String> getHeaders(RemoteBehavior remoteBehavior) {
        Map<String, String> headers = super.getHeaders(remoteBehavior);
        headers.put(ExtendedHttpHeader.X_TTL.toString(), String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));
        return headers;
    }

    @Override
    protected byte[] getBody() {
        return value;
    }

    @Override
    public Object handleResponse(ServiceClientResponse response) throws IOException {
        if (response.getStatusCode() != HttpStatusCode.CREATED.intValue() &&
                response.getStatusCode() != HttpStatusCode.OK.intValue()) {
            throw new DatastoreOperationException("Remote request failed with: " + response.getStatusCode());
        }

        return Boolean.TRUE;
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.put(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior), value);
    }

}
