package com.rackspace.papi.service.authclient.akka;

import java.util.Map;

public class AuthGetRequest {

    private String uri;
    private Map<String, String> headers;
    private String token;

    public AuthGetRequest(String token, String uri, Map<String, String> headers) {
        this.uri = uri;
        this.headers = headers;
        this.token = token;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getToken() {
        return token;
    }

}
