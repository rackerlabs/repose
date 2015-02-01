package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.services.datastore.distributed.RemoteBehavior;

import javax.servlet.http.HttpServletResponse;
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
        return Boolean.valueOf(response.getStatus() == HttpServletResponse.SC_ACCEPTED);
    }
}
