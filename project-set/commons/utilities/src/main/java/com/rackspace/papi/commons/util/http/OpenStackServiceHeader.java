package com.rackspace.papi.commons.util.http;

/**
 * Enum for Headers added by the Client Authentication componenet
 */
public enum OpenStackServiceHeader implements HeaderConstant {
    /**
     * The client identity being passed in
     */
    EXTENDED_AUTHORIZATION("X-Authorization"),

    /**
     *  'Confirmed' or 'Invalid'
     *   The underlying service will only see a value of 'Invalid' if PAPI
     *   is configured to run in 'delegable' mode
     */
    IDENTITY_STATUS("X-Identity-Status"),

    /**
     * Unique user identifier, string
     */
    USER_NAME("X-User-Name"),

    /**
     * Identity-service managed unique identifier, string
     */
    USER_ID("X-User-Id"),

    /**
     * Unique tenant identifier, string
     */
    TENANT_NAME("X-Tenant-Name"),

    /**
     * Identity service managed unique identifier, string
     */    
    TENANT_ID("X-Tenant-Id"),

    /**
     * Comma delimited list of case-sensitive Roles
     */
    ROLES("X-Roles"),
    
    IMPERSONATOR_ID("X-Impersonator-Id"),
    IMPERSONATOR_NAME("X-Impersonator-Name"),
    
    DEFAULT_REGION("X-Default-Region"),
    
    X_EXPIRATION("x-token-expires");

    
    private final String headerKey;

    private OpenStackServiceHeader(String headerKey) {
        this.headerKey = headerKey.toLowerCase();
    }

    @Override
    public String toString() {
        return headerKey;
    }
    
    @Override
    public boolean matches(String st) {
        return headerKey.equalsIgnoreCase(st);
    }
}
