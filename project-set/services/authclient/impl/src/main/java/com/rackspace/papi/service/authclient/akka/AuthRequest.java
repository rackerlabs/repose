package com.rackspace.papi.service.authclient.akka;

import java.util.Map;

public class AuthRequest {

    private String uri;
    private Map<String, String> headers;

    public AuthRequest(String uri, Map<String, String> headers) {
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
