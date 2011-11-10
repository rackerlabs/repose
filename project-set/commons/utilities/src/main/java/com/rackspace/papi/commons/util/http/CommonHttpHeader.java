package com.rackspace.papi.commons.util.http;

/**
 *
 * 
 */
public enum CommonHttpHeader implements HttpHeader {
    
    //Auth specific
    AUTH_TOKEN("X-Auth-Token"),
    AUTHORIZATION("Authorization"),

    /**
     * This header allows the underlying service to identify who authenticated
     * the request in question.
     */
    EXTENDED_AUTHORIZATION("X-AUTHORIZATION"),
    
    /**
     * This header allows the underlying service to identify whether or not the
     * request was correctly authenticated.
     * 
     * The only two valid values for this header should be: "Confirmed" or "Indeterminate"
     */
    IDENTITY_STATUS("X_IDENTITY_STATUS"),
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