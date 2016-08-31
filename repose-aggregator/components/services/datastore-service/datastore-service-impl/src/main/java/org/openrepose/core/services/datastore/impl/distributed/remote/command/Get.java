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

import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * @author zinic
 */
public class Get extends AbstractRemoteCommand {
    private final ObjectSerializer objectSerializer = new ObjectSerializer(this.getClass().getClassLoader());

    public Get(String cacheObjectKey, InetSocketAddress remoteEndpoint, String connPoolId, boolean useHttps) {
        super(cacheObjectKey, remoteEndpoint, connPoolId, useHttps);
    }

    @Override
    public ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior) {
        return proxyService.get(getBaseUrl(), getCacheObjectKey(), getHeaders(remoteBehavior), getConnectionPoolId());
    }

    @Override
    public Object handleResponse(ServiceClientResponse response) throws IOException {
        final int statusCode = response.getStatus();

        if (statusCode == HttpServletResponse.SC_OK) {
            final InputStream internalStreamReference = response.getData();

            try {
                return objectSerializer.readObject(RawInputStreamReader.instance().readFully(internalStreamReference));
            } catch (ClassNotFoundException cnfe) {
                throw new DatastoreOperationException("Unable to marshall a java object from stored element contents. Reason: " + cnfe.getMessage(), cnfe);
            }
        } else if (statusCode == HttpServletResponse.SC_NOT_FOUND) {
            return null;
        }

        throw new DatastoreOperationException("Remote request failed with: " + statusCode);
    }
}
