package com.rackspace.papi.commons.util.http;

/**
 *
 * @author jhopper
 */
public enum PowerApiHeader implements HttpHeader {

    ROUTE_DESTINATION("X-PP-RouteDestination"),
    USER("HTTP_X_USER"),
    GROUPS("HTTP_X_ROLE"),
    TENANT("HTTP_X_TENANT"),
    TENANT_NAME("HTTP_X_TENANT_NAME"),
    TENANT_ID("HTTP_X_TENANT_ID"),
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
