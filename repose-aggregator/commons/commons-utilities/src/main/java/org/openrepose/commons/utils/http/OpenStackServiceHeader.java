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
 * Constants for Headers added by the Client Authentication component
 */
public class OpenStackServiceHeader {
    /**
     * The client identity being passed in
     */
    public static final String EXTENDED_AUTHORIZATION = "X-Authorization";

    /**
     * 'Confirmed' or 'Invalid'
     * The underlying service will only see a value of 'Invalid' if PAPI
     * is configured to run in 'delegable' mode
     */
    public static final String IDENTITY_STATUS = "X-Identity-Status";

    /**
     * ID of the domain to which the user belongs in Identity as a string.
     */
    public static final String DOMAIN_ID = "X-Domain-Id";

    /**
     * Unique user identifier, string
     */
    public static final String USER_NAME = "X-User-Name";

    /**
     * Identity-service managed unique identifier, string
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * Unique tenant identifier, string
     */
    public static final String TENANT_NAME = "X-Tenant-Name";

    /**
     * Identity service managed unique identifier, string
     */
    public static final String TENANT_ID = "X-Tenant-Id";

    /**
     * JSON map of tenant IDs to role names.
     */
    public static final String TENANT_ROLES_MAP = "X-Map-Roles";

    /**
     * Comma delimited list of case-sensitive Roles
     */
    public static final String ROLES = "X-Roles";

    /**
     * Comma-delimited list of authentication methods used
     */
    public static final String AUTHENTICATED_BY = "X-Authenticated-By";

    public static final String IMPERSONATOR_ID = "X-Impersonator-Id";
    public static final String IMPERSONATOR_NAME = "X-Impersonator-Name";
    public static final String IMPERSONATOR_ROLES = "X-Impersonator-Roles";

    public static final String DEFAULT_REGION = "X-Default-Region";

    public static final String X_EXPIRATION = "x-token-expires";
    public static final String CONTACT_ID = "X-CONTACT-ID";

    private OpenStackServiceHeader() {
        // This class should not be instantiated.
    }

    /**
     * This keeps it compatible with the old Enumeration way of doing business.
     *
     * @return an array of all the constants defined by this class
     */
    public static String[] values() {
        return new String[]{
                EXTENDED_AUTHORIZATION,
                IDENTITY_STATUS,
                USER_NAME,
                USER_ID,
                TENANT_NAME,
                TENANT_ID,
                ROLES,
                AUTHENTICATED_BY,
                IMPERSONATOR_ID,
                IMPERSONATOR_NAME,
                IMPERSONATOR_ROLES,
                DEFAULT_REGION,
                X_EXPIRATION,
                CONTACT_ID
        };
    }
}
