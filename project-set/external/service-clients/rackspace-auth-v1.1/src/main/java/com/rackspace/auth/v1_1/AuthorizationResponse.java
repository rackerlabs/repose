package com.rackspace.auth.v1_1;

/**
 *
 * @author jhopper
 */
public class AuthorizationResponse {

    private final int responseCode;    
    private final boolean allowed;

    public AuthorizationResponse(int responseCode, boolean allowed) {
        this.responseCode = responseCode;
        this.allowed = allowed;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public boolean authorized() {
        return allowed;
    }
}
