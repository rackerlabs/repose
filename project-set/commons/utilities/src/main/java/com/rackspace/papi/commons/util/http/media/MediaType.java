package com.rackspace.papi.commons.util.http.media;

/**
 *
 * 
 */
public enum MediaType {

    APPLICATION_XML("application/xml"),
    APPLICATION_JSON("application/json"),
    APPLICATION_ATOM_XML("application/atom+xml"),
    APPLICATION_XHTML_XML("application/xhtml+xml"),
    TEXT_HTML("text/html"),
    TEXT_PLAIN("text/plain"),
    WILDCARD("*/*"),
    UNKNOWN(""),
    UNSPECIFIED("");

    public static MediaType fromMediaTypeString(String mimeType) {
        for (MediaType ct : values()) {
            if (ct.toString().equalsIgnoreCase(mimeType)) {
                return ct;
            }
        }
                
        return UNKNOWN;
    }

    private final String mimeType;

    private MediaType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return mimeType;
    }
}
