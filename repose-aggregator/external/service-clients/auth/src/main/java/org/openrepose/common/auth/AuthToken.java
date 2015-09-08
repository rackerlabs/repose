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
package org.openrepose.common.auth;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Set;

/**
 * Abstract class to encapsulate information from the validatetoken calls from the  Authentication Service client {@link org.openrepose.common.auth.openstack.AuthenticationService}
 */
public abstract class AuthToken implements Serializable {

    public abstract String getTenantId();

    public abstract void setMatchingTenantId(String tenantId);

    public abstract String getMatchingTenantId();

    public abstract String getUserId();

    public abstract String getTokenId();

    public abstract String getUsername();

    public abstract String getRoles();

    public abstract long getExpires();

    public abstract String getImpersonatorTenantId();

    public abstract String getImpersonatorUsername();

    public abstract Set<String> getImpersonatorRoles();

    public abstract String getTenantName();

    public abstract String getDefaultRegion();

    public abstract Set<String> getTenantIds();

    public abstract String getContactId();


    public Long tokenTtl() {
        long ttl = 0;

        if (getExpires() > 0) {
            ttl = getExpires() - Calendar.getInstance().getTimeInMillis();
        }

        return ttl > 0 ? ttl : 0;
    }

    public int safeTokenTtl() {
        Long tokenTtl = tokenTtl();

        if (tokenTtl >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return tokenTtl.intValue();
    }
}
