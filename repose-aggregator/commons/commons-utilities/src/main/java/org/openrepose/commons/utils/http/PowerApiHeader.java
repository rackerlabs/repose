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
 * @author jhopper
 */
public class PowerApiHeader {

    public static final String NEXT_ROUTE = "X-PP-Next-Route";
    public static final String USER = "X-PP-User";
    public static final String GROUPS = "X-PP-Groups";
    public static final String DOMAIN = "X-Domain";
    public static final String X_CATALOG = "x-catalog";
    public static final String RELEVANT_ROLES = "X-Relevant-Roles";
    public static final String TRACE_REQUEST = "x-trace-request";

    private PowerApiHeader() {
        // This class should not be instantiated.
    }

    /**
     * This keeps it compatible with the old Enumeration way of doing business.
     *
     * @return an array of all the constants defined by this class
     */
    public static String[] values() {
        return new String[]{
                NEXT_ROUTE,
                USER,
                GROUPS,
                DOMAIN,
                X_CATALOG,
                TRACE_REQUEST
        };
    }
}
