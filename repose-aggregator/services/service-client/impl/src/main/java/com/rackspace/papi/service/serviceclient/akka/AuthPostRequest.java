package com.rackspace.papi.service.serviceclient.akka;

import akka.routing.ConsistentHashingRouter.ConsistentHashable;

import javax.ws.rs.core.MediaType;
import java.util.Map;

public class AuthPostRequest implements ConsistentHashable {

    private String uri;
    private Map<String, String> headers;
    private String hashKey;
    private String payload;
    private MediaType contentMediaType;
    private MediaType acceptMediaType;

    public AuthPostRequest(String hashKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType, MediaType acceptMediaType) {
        this.uri = uri;
        this.headers = headers;
        this.payload = payload;
        this.hashKey = hashKey;
        this.contentMediaType = contentMediaType;
        this.acceptMediaType = acceptMediaType;
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

    public MediaType getContentMediaType() {
        return contentMediaType;
    }

    public void setContentMediaType(MediaType contentMediaType) {
        this.contentMediaType = contentMediaType;
    }

    public MediaType getAcceptMediaType() {
        return acceptMediaType;
    }

    public void setAcceptMediaType(MediaType acceptMediaType) {
        this.acceptMediaType = acceptMediaType;
    }
}
