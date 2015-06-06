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
package org.openrepose.core.services.httpclient;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.ConnPoolControl;

public interface ExtendedHttpClient {
    /**
     * @return the wrapped {@link CloseableHttpClient}
     */
    CloseableHttpClient getHttpClient();

    /**
     * @return the {@link MessageConstraints} used to construct the wrapped {@link CloseableHttpClient}
     */
    MessageConstraints getMessageConstraints();

    /**
     * @return the {@link RequestConfig} used to construct the wrapped {@link CloseableHttpClient}
     */
    RequestConfig getRequestConfig();

    /**
     * @return the {@link ConnectionConfig} used to construct the wrapped {@link CloseableHttpClient}
     */
    ConnectionConfig getConnectionConfig();

    /**
     * @return the {@link SocketConfig} used to construct the wrapped {@link CloseableHttpClient}
     */
    SocketConfig getSocketConfig();

    /**
     * @return the {@link ConnPoolControl} which provides control over the connection pool used by the wrapped {@link CloseableHttpClient}
     */
    PoolingHttpClientConnectionManager getConnectionManager();

    /**
     * @return the {@link ConnectionKeepAliveStrategy} used to construct the wrapped {@link CloseableHttpClient}
     */
    ConnectionKeepAliveStrategy getKeepAliveStrategy();

    /**
     * @return a string which uniquely identifies the wrapped {@link CloseableHttpClient}
     */
    String getClientInstanceId();

    /**
     * @return a boolean indicating whether or not chunked encoding should be used
     */
    boolean getChunkedEncoding();
}
