package com.rackspace.papi.components.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public class Delete extends AbstractRemoteCommand {

    public Delete(String cacheObjectKey, InetSocketAddress remoteEndpoint) {
        super(cacheObjectKey, remoteEndpoint);
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.delete(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior));
    }

    @Override
    public Object handleResponse(ServiceClientResponse response) throws IOException {
        return Boolean.valueOf(response.getStatusCode() == HttpStatusCode.ACCEPTED.intValue());
    }
}
