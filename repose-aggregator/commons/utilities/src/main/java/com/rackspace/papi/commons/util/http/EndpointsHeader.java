package com.rackspace.papi.commons.util.http;

/**
 * This enum matches the pattern used by other special header addition enums.
 * */

public enum EndpointsHeader implements HeaderConstant {
    //header key
    X_CATALOG("x-catalog");

    private final String headerKey;

    private EndpointsHeader(String headerKey) {
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
