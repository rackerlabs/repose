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
package org.openrepose.core.services.httpclient.impl;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.ConnPoolControl;

/**
 * A POJO to hold a HttpClient and related information
 */
public class ExtendedHttpClient {
    private final CloseableHttpClient httpClient;
    private final RequestConfig requestConfig;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final String clientInstanceId;
    private final boolean chunkedEncoding;

    public ExtendedHttpClient(CloseableHttpClient httpClient,
                              RequestConfig requestConfig,
                              PoolingHttpClientConnectionManager connectionManager,
                              String clientInstanceId,
                              boolean chunkedEncoding) {
        this.httpClient = httpClient;
        this.requestConfig = requestConfig;
        this.connectionManager = connectionManager;
        this.clientInstanceId = clientInstanceId;
        this.chunkedEncoding = chunkedEncoding;
    }

    /**
     * @return the wrapped {@link CloseableHttpClient}
     */
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * @return the {@link RequestConfig} used to construct the wrapped {@link CloseableHttpClient}
     */
    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    /**
     * @return the {@link ConnPoolControl} which provides control over the connection pool used by the wrapped {@link CloseableHttpClient}
     */
    public PoolingHttpClientConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * @return a string which uniquely identifies the wrapped {@link CloseableHttpClient}
     */
    public String getClientInstanceId() {
        return clientInstanceId;
    }

    /**
     * @return a boolean indicating whether or not chunked encoding should be used
     */
    public boolean getChunkedEncoding() {
        return chunkedEncoding;
    }
}
