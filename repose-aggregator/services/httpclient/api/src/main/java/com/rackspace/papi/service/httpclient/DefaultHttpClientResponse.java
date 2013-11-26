package com.rackspace.papi.service.httpclient;

import org.apache.http.client.HttpClient;

/**
 *  An HttpClientResponse that generates a unique UUID
 */
public class DefaultHttpClientResponse implements HttpClientResponse {

    private HttpClient httpClient;
    private String clientId;
    private String userId;

    public DefaultHttpClientResponse(HttpClient httpClient, String clientId, String userId) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.userId = userId;
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserId() {
        return userId;
    }
}
