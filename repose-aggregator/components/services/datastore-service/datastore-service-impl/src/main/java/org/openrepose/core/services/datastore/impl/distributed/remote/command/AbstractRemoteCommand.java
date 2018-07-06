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

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest;
import org.openrepose.core.services.datastore.impl.distributed.DatastoreHeader;
import org.openrepose.core.services.datastore.impl.distributed.remote.RemoteCommand;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractRemoteCommand implements RemoteCommand {

    private final InetSocketAddress remoteEndpoint;
    private final String cacheObjectKey;
    private String connPoolId;
    private boolean useHttps;

    public AbstractRemoteCommand(String cacheObjectKey, InetSocketAddress remoteEndpoint, String connPoolId, boolean useHttps) {
        this.cacheObjectKey = cacheObjectKey;
        this.remoteEndpoint = remoteEndpoint;
        this.connPoolId = connPoolId;
        this.useHttps = useHttps;
    }

    public String getUrl() {
        return CacheRequest.urlFor(getRemoteEndpoint(), getCacheObjectKey(), useHttps);
    }

    public String getBaseUrl() {
        return CacheRequest.urlFor(remoteEndpoint, useHttps);
    }

    public String getConnectionPoolId() {
        return connPoolId;
    }

    protected byte[] getBody() {
        return new byte[0];
    }

    protected Map<String, String> getHeaders(RemoteBehavior remoteBehavior) {
        Map<String, String> headers = new HashMap<>();
        headers.put(DatastoreHeader.REMOTE_BEHAVIOR, remoteBehavior.name());

        Optional.ofNullable(MDC.get(PowerApiHeader.TRACE_REQUEST))
            .ifPresent(value -> headers.put(PowerApiHeader.TRACE_REQUEST, value));

        return headers;
    }

    protected InetSocketAddress getRemoteEndpoint() {
        return remoteEndpoint;
    }

    protected String getCacheObjectKey() {
        return cacheObjectKey;
    }
}
