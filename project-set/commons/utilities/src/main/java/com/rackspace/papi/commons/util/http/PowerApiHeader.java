package com.rackspace.papi.commons.util.http;

/**
 *
 * @author jhopper
 */
public enum PowerApiHeader implements HeaderConstant {

    NEXT_ROUTE("X-PP-Next-Route"),
    USER("X-PP-User"),
    GROUPS("X-PP-Groups");
    
    private final String headerKey;

    private PowerApiHeader(String headerKey) {
        this.headerKey = headerKey;
    }

    @Override
    public String getHeaderKey() {
        return headerKey;
    }
    
    @Override
    public boolean matches(String st) {
        return headerKey.equalsIgnoreCase(st);
    }
    
    @Override
    public String toString() {
        return headerKey.toLowerCase();
    }
}
