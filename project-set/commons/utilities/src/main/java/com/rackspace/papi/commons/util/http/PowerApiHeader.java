package com.rackspace.papi.commons.util.http;

/**
 *
 * @author jhopper
 */
public enum PowerApiHeader implements HttpHeader {

    ROUTE_DESTINATION("X-PP-RouteDestination"),
    USER("X_USER"),
    GROUPS("X_ROLE"),
    TENANT("X_TENANT"),
    TENANT_NAME("X_TENANT_NAME"),
    TENANT_ID("X_TENANT_ID"),
    ROLES("X_ROLE");

    private final String headerName;

    private PowerApiHeader(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String headerKey() {
        return headerName;
    }
}
