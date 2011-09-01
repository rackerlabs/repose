package com.rackspace.papi.commons.util.http;

/**
 *
 * @author jhopper
 */
public enum PowerApiHeader implements HttpHeader {

    ROUTE_DESTINATION("X-PP-RouteDestination"),
    USER("X-PP-User"),
    GROUPS("X-PP-Groups");

    private final String headerName;

    private PowerApiHeader(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String headerKey() {
        return headerName;
    }
}
