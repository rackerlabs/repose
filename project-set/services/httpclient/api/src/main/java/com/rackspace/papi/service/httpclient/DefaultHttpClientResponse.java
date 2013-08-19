package com.rackspace.papi.service.httpclient;

import org.apache.http.client.HttpClient;

import java.util.UUID;

/**
 *  An HttpClientResponse that generates a unique UUID
 */
public class DefaultHttpClientResponse implements HttpClientResponse{

    private HttpClient httpClient;
    private String clientId;
    private String uuid;

    public DefaultHttpClientResponse(HttpClient httpClient, String clientId) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUuid() {
        return uuid;
    }
}
