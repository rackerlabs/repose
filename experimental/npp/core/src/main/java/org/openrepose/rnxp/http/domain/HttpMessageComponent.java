package org.openrepose.rnxp.http.domain;

/**
 * Mesage Start:    -1
 * Request Line:    0-100
 * Status Line:     101-200
 * Header:          300-400
 * Content:         400-500
 * 
 * @author zinic
 */
public enum HttpMessageComponent {
    
    // Connection Envlope
    MESSAGE_START,
    MESSAGE_END,
    
    // HTTP Message
    HTTP_VERSION,
    HEADER,
    CONTENT_START,
    CONTENT_END,
    
    // Request
    REQUEST_METHOD,
    REQUEST_URI,
        
    // Response
    RESPONSE_STATUS_CODE
}
