/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.http;

/**
 * Enum for Headers added by the Client Authentication componenet
 */
public enum OpenStackServiceHeader implements HeaderConstant {
    /**
     * The client identity being passed in
     */
    EXTENDED_AUTHORIZATION("X-Authorization"),

    /**
     * 'Confirmed' or 'Invalid'
     * The underlying service will only see a value of 'Invalid' if PAPI
     * is configured to run in 'delegable' mode
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

    /**
     * Comma-delimited list of authentication methods used
     */
    AUTHENTICATED_BY("X-Authenticated-By"),

    IMPERSONATOR_ID("X-Impersonator-Id"),
    IMPERSONATOR_NAME("X-Impersonator-Name"),
    IMPERSONATOR_ROLES("X-Impersonator-Roles"),

    DEFAULT_REGION("X-Default-Region"),

    X_EXPIRATION("x-token-expires"),
    CONTACT_ID("X-CONTACT-ID");


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
