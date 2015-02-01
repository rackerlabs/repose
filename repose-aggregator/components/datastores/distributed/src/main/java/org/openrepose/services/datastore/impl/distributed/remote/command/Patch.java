package org.openrepose.services.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.datastore.distributed.RemoteBehavior;
import org.openrepose.services.datastore.distributed.SerializablePatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    private final Logger LOG = LoggerFactory.getLogger(Patch.class);

    private final TimeUnit timeUnit;
    private final byte[] value;
    private final int ttl;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public Patch(TimeUnit timeUnit, SerializablePatch patch, int ttl, String cacheObjectKey, InetSocketAddress remoteEndpoint) {
        super(cacheObjectKey, remoteEndpoint);
        this.timeUnit = timeUnit;
        this.ttl = ttl;
        byte[] deferredValue = null;
        try {
            deferredValue = ObjectSerializer.instance().writeObject(patch);
        } catch (IOException ioe) {
            LOG.warn("unable to serialize object", ioe);
        }
        this.value = deferredValue;
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
        if (response.getStatus() == HttpServletResponse.SC_OK) {
            final InputStream internalStreamReference = response.getData();

             try {
                 return ObjectSerializer.instance().readObject(RawInputStreamReader.instance().readFully(internalStreamReference));
             } catch (ClassNotFoundException cnfe) {
                 throw new DatastoreOperationException("Unable to marshall a java object from stored element contents. Reason: " + cnfe.getMessage(), cnfe);
             }
        } else {
            throw new DatastoreOperationException("Remote request failed with: " + response.getStatus());
        }
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.patch(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior), getBody());
    }

}
