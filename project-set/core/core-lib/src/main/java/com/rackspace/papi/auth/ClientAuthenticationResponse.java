package com.rackspace.papi.auth;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public class ClientAuthenticationResponse {

    public static final ClientAuthenticationResponse NO_AUTH_AVAILABLE = new ClientAuthenticationResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, "Unable to authenticate request");
    
    private final Map<String, String> headersToAdd;

    private HttpStatusCode statusCode;
    private String message;
    
    public ClientAuthenticationResponse(HttpStatusCode statusCode, String message) {
        headersToAdd = new HashMap<String, String>();
        
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public boolean isAuthenticated() {
        return statusCode == HttpStatusCode.OK;
    }
    
    public boolean hasMessage() {
        return message != null;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatusCode(HttpStatusCode statusCode) {
        this.statusCode = statusCode;
    }
    
    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public void addHeader(String key, String value) {
        headersToAdd.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headersToAdd;
    }
}
