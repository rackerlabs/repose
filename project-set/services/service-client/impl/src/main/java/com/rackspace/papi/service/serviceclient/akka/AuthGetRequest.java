package com.rackspace.papi.service.serviceclient.akka;

import akka.routing.ConsistentHashingRouter.ConsistentHashable;

import java.util.Map;

public class AuthGetRequest implements ConsistentHashable {

    private String uri;
    private Map<String, String> headers;
    private String hashKey;

    public AuthGetRequest(String hashKey, String uri, Map<String, String> headers) {
        this.uri = uri;
        this.headers = headers;
        this.hashKey = hashKey;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String hashKey() {
        return hashKey;
    }

    public String consistentHashKey(){
        return hashKey();
    }

}
