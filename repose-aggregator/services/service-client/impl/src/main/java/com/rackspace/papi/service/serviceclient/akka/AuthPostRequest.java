package com.rackspace.papi.service.serviceclient.akka;

import akka.routing.ConsistentHashingRouter.ConsistentHashable;

import java.util.Map;

public class AuthPostRequest implements ConsistentHashable {

    private String uri;
    private Map<String, String> headers;
    private String hashKey;
    private String payload;

    public AuthPostRequest(String hashKey, String uri, Map<String, String> headers, String payload) {
        this.uri = uri;
        this.headers = headers;
        this.payload = payload;
        this.hashKey = hashKey;
    }

    public String getUri() {
        return uri;
    }

    public String getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String consistentHashKey() {
        return hashKey;
    }

}
