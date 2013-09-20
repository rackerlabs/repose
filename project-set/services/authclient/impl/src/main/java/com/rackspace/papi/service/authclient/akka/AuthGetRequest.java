package com.rackspace.papi.service.authclient.akka;

import java.util.Map;

public class AuthGetRequest {

    private String uri;
    private Map<String, String> headers;

    public AuthGetRequest(String uri, Map<String, String> headers) {
        this.uri = uri;
        this.headers = headers;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
