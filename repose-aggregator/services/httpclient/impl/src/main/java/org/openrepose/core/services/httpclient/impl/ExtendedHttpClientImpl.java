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
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.openrepose.core.services.httpclient.ExtendedHttpClient;

/**
 * A POJO to hold a HttpClient and related information
 */
public class ExtendedHttpClientImpl implements ExtendedHttpClient {
    private final CloseableHttpClient httpClient;
    private final MessageConstraints messageConstraints;
    private final RequestConfig requestConfig;
    private final ConnectionConfig connectionConfig;
    private final SocketConfig socketConfig;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final ConnectionKeepAliveStrategy keepAliveStrategy;
    private final String clientInstanceId;
    private final boolean chunkedEncoding;

    public ExtendedHttpClientImpl(CloseableHttpClient httpClient,
                                  MessageConstraints messageConstraints,
                                  RequestConfig requestConfig,
                                  ConnectionConfig connectionConfig,
                                  SocketConfig socketConfig,
                                  PoolingHttpClientConnectionManager connectionManager,
                                  ConnectionKeepAliveStrategy keepAliveStrategy,
                                  String clientInstanceId,
                                  boolean chunkedEncoding) {
        this.httpClient = httpClient;
        this.messageConstraints = messageConstraints;
        this.requestConfig = requestConfig;
        this.connectionConfig = connectionConfig;
        this.socketConfig = socketConfig;
        this.connectionManager = connectionManager;
        this.keepAliveStrategy = keepAliveStrategy;
        this.clientInstanceId = clientInstanceId;
        this.chunkedEncoding = chunkedEncoding;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public MessageConstraints getMessageConstraints() {
        return messageConstraints;
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public PoolingHttpClientConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public ConnectionKeepAliveStrategy getKeepAliveStrategy() {
        return keepAliveStrategy;
    }

    public String getClientInstanceId() {
        return clientInstanceId;
    }

    public boolean getChunkedEncoding() {
        return chunkedEncoding;
    }
}
