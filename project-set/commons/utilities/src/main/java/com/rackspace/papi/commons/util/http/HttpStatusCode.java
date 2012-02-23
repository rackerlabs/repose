package com.rackspace.papi.commons.util.http;

/**
 * This class is an enumeration of HTTP response status codes
 */
public enum HttpStatusCode {

    OK(200),
    ACCEPTED(202),
    NO_CONTENT(204),
    MULTIPLE_CHOICES(300),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    REQUEST_ENTITY_TOO_LARGE(413),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    HTTP_VERSION_NOT_SUPPORTED(505),

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
