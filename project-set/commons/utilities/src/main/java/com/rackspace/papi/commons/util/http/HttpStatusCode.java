/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.http;

/**
 * This class is an enumeration of HttpStatusCodes that we use.  This was created as an
 * alternative to constants in HttpServletResponse interface.
 */
public enum HttpStatusCode {

    OK(200),
    ACCEPTED(202),
    MULTIPLE_CHOICES(300),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    REQUEST_ENTITY_TOO_LARGE(413),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),

    UNSUPPORTED_RESPONSE_CODE(-1);

    
    private final int statusCode;

    private HttpStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int intValue() {
        return statusCode;
    }
    
    public static HttpStatusCode fromInt(int i) {
        for (HttpStatusCode code : values()) {
            if (code.statusCode == i) {
                return code;
            }
        }

        return UNSUPPORTED_RESPONSE_CODE;
    }
}
