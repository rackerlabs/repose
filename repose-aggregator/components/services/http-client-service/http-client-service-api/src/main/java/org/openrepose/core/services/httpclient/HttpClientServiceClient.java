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

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.UUID;

/**
 * Acts as a client for the {@link HttpClientService}.
 * This client handles user management so that the end-user does not have to.
 * User management is necessary to support decommissioning, which is ultimately necessary to support updating clients.
 * <p>
 * The actual behavior of this class is to call through to the HTTP client service to acquire the most recently
 * configured HTTP client for the provided {@code clientId}.
 * Since the client is acquired for each request, it is effectively a single-use client.
 * In addition to providing an up-to-date HTTP client, this also ensures that connections are not leaked.
 * <p>
 * Extends {@link CloseableHttpClient} to provide a friendly, familiar, and full-featured API.
 */
@RequiredArgsConstructor
public class HttpClientServiceClient extends CloseableHttpClient {

    private final InternalHttpClientService httpClientService;
    private final HttpClientUserManager httpClientUserManager;
    private final String clientId;

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        InternalHttpClient internalHttpClient = httpClientService.getInternalClient(clientId);
        String userId = UUID.randomUUID().toString();
        httpClientUserManager.registerUser(internalHttpClient.getInstanceId(), userId);
        try {
            return internalHttpClient.getClient().execute(target, request, context);
        } finally {
            httpClientUserManager.deregisterUser(internalHttpClient.getInstanceId(), userId);
        }
    }

    @Override
    @Deprecated
    public HttpParams getParams() {
        return httpClientService.getInternalClient(clientId).getClient().getParams();
    }

    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager() {
        return httpClientService.getInternalClient(clientId).getClient().getConnectionManager();
    }

    @Override
    public void close() throws IOException {
        // Intentional no-op to prevent the end-user from closing the HTTP client
        // since the HTTP client lifecycle is being managed by the HTTP client service.
    }
}
