package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.papi.commons.util.http.HttpHeader;

/**
 * @author fran
 */
public enum OpenStackServiceHeader implements HttpHeader {
    /**
     * The client identity being passed in
     */
    EXTENDED_AUTHORIZATION("HTTP_X_AUTHORIZATION"),

    /**
     *  'Confirmed' or 'Invalid'
     *   The underlying service will only see a value of 'Invalid' if the PAPI
     *   is configured to run in 'delegatable' mode
     */
    IDENTITY_STATUS("HTTP_X_IDENTITY_STATUS"),

    /**
     * Unique user identifier, string
     */
    USER_NAME("HTTP_X_USER_NAME"),

    /**
     * Identity-service managed unique identifier, string
     */
    USER_ID("HTTP_X_USER_ID"),

    /**
     * Unique tenant identifier, string
     */
    TENANT_NAME("HTTP_X_TENANT_NAME"),

    /**
     * Identity service managed unique identifier, string
     */    
    TENANT_ID("HTTP_X_TENANT_ID"),

    /**
     * Comma delimited list of case-sensitive Roles
     */
    ROLES("X_ROLES");

    private final String headerName;

    private OpenStackServiceHeader(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String headerKey() {
        return headerName;
    }
}
