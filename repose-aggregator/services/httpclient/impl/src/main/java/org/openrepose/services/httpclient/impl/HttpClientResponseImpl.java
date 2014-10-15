package org.openrepose.services.httpclient.impl;

import org.openrepose.services.httpclient.HttpClientResponse;
import org.apache.http.client.HttpClient;

/**
 *  An HttpClientResponse that generates a unique UUID
 */
public class HttpClientResponseImpl implements HttpClientResponse {

    private HttpClient httpClient;
    private String clientId;
    private String clientInstanceId;
    private String userId;

    public HttpClientResponseImpl(HttpClient httpClient, String clientId, String clientInstanceId, String userId) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.clientInstanceId = clientInstanceId;
        this.userId = userId;
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    public String getClientInstanceId() {
        return clientInstanceId;
    }

    public String getUserId() {
        return userId;
    }

}
