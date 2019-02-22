/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.impl.distributed.remote.command;

import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;
import org.openrepose.core.services.datastore.distributed.SerializablePatch;
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
    private static final Logger LOG = LoggerFactory.getLogger(Patch.class);

    private final TimeUnit timeUnit;
    private final byte[] value;
    private final int ttl;

    private final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader());

    public Patch(TimeUnit timeUnit, SerializablePatch patch, int ttl, String cacheObjectKey, InetSocketAddress remoteEndpoint, String connPoolId, boolean useHttps) {
        super(cacheObjectKey, remoteEndpoint, connPoolId, useHttps);
        this.timeUnit = timeUnit;
        this.ttl = ttl;
        byte[] deferredValue = null;
        try {
            deferredValue = objectSerializer.writeObject(patch);
        } catch (IOException ioe) {
            LOG.warn("unable to serialize object", ioe);
        }
        this.value = deferredValue;
    }

    @Override
    protected Map<String, String> getHeaders(RemoteBehavior remoteBehavior) {
        Map<String, String> headers = super.getHeaders(remoteBehavior);
        headers.put(ExtendedHttpHeader.X_TTL, String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));
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
                return objectSerializer.readObject(RawInputStreamReader.instance().readFully(internalStreamReference));
            } catch (ClassNotFoundException cnfe) {
                throw new DatastoreOperationException("Unable to marshall a java object from stored element contents. Reason: " + cnfe.getMessage(), cnfe);
            }
        } else {
            throw new DatastoreOperationException("Remote request failed with: " + response.getStatus());
        }
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.patch(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior), getBody(), getConnectionPoolId());
    }

}
