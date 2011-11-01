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

    public String getType() {
        String type = "";
        String[] parts = this.mimeType.split("/");

        if (parts.length > 1) {
            type = parts[0];
        }

        return type;
    }

    public String getSubtype() {
        String subtype = "";
        String[] parts = this.mimeType.split("/");

        if (parts.length > 1) {
            subtype = parts[1];    
        }

        return subtype;
    }
}
