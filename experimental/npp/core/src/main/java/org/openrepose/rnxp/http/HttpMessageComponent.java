package org.openrepose.rnxp.http;

/**
 */
public enum HttpMessageComponent {
    
    // Connection Envlope
    MESSAGE_START,
    MESSAGE_END,
    
    // HTTP Message
    HTTP_VERSION,
    HEADER,
    CONTENT_START,
    CONTENT,
    
    // Request
    REQUEST_METHOD,
    REQUEST_URI,
        
    // Response
    RESPONSE_STATUS_CODE
}
