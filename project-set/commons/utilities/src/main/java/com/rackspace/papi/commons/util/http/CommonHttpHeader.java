package com.rackspace.papi.commons.util.http;

/**
 *
 * 
 */
public enum CommonHttpHeader implements HttpHeader {
    
    //Auth specific
    AUTH_TOKEN("X-Auth-Token"),
    AUTHORIZATION("Authorization"),    
    WWW_AUTHENTICATE("WWW-Authenticate"),
    
    //Standards
    HOST("Host"),
    RETRY_AFTER("Retry-After"),
    
    //Content specific
    ACCEPT("Accept"),
    CONTENT_TYPE("Content-Type");

    private final String headerName;

    private CommonHttpHeader(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String headerKey() {
        return headerName;
    }
}