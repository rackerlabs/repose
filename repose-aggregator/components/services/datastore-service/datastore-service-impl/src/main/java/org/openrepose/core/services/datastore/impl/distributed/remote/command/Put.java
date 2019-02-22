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
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Put extends AbstractRemoteCommand {

    private final TimeUnit timeUnit;
    private final Serializable value;
    private final int ttl;
    private final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader());

    public Put(TimeUnit timeUnit, Serializable value, int ttl, String cacheObjectKey, InetSocketAddress remoteEndpoint, String connPoolId, boolean useHttps) {
        super(cacheObjectKey, remoteEndpoint, connPoolId, useHttps);
        this.timeUnit = timeUnit;
        this.ttl = ttl;
        this.value = value;
    }

    @Override
    protected Map<String, String> getHeaders(RemoteBehavior remoteBehavior) {
        Map<String, String> headers = super.getHeaders(remoteBehavior);
        headers.put(ExtendedHttpHeader.X_TTL, String.valueOf(TimeUnit.SECONDS.convert(ttl, timeUnit)));
        return headers;
    }

    @Override
    protected byte[] getBody() {
        try {
            return objectSerializer.writeObject(value);
        } catch (IOException ioe) {
            throw new DatastoreOperationException("Failed to serialize value to be put", ioe);
        }
    }

    @Override
    public Object handleResponse(ServiceClientResponse response) throws IOException {
        if (response.getStatus() != HttpServletResponse.SC_ACCEPTED) {
            throw new DatastoreOperationException("Remote request failed with: " + response.getStatus());
        }

        return Boolean.TRUE;
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.put(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior), getBody(), getConnectionPoolId());
    }

}
