/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.common.auth.openstack;

import java.util.Calendar;

public class AdminToken {

    /**
     * A convienient location for the admin token cache key used internally to repose.
     */
    public static final String CACHE_KEY = "ADMIN_TOKEN";

    private final String token;
    private final Calendar expires;

    public AdminToken(String token, Calendar expires) {
        this.token = token;
        this.expires = expires;
    }

    public String getToken() {
        return token;
    }

    public boolean isValid() {
        return expires != null && !expires.getTime().before(Calendar.getInstance().getTime());
    }
}
