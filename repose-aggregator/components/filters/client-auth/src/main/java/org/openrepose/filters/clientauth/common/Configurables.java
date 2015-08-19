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
package org.openrepose.filters.clientauth.common;

import org.openrepose.commons.utils.regex.KeyedRegexExtractor;

import java.util.List;

/**
 * @author fran
 *         <p/>
 *         This class manages information from config files related to auth.
 */
@Deprecated
public class Configurables {
    private final boolean delegable;
    private final double delegableQuality;
    private final String authServiceUri;
    private final KeyedRegexExtractor<String> keyedRegexExtractor;
    private final boolean tenanted;
    private final long groupCacheTtl;
    private final long userCacheTtl;
    private final long tokenCacheTtl;
    private final int cacheOffset;
    private final boolean requestGroups;
    private final EndpointsConfiguration endpointsConfiguration;
    private final List<String> serviceAdminRoles;
    private final List<String> ignoreTenantRoles;
    private final boolean sendAllTenantIds;
    private final boolean sendTenantIdQuality;

    public Configurables(boolean delegable, double delegableQuality, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor,
                         boolean tenanted, long groupCacheTtl, long tokenCacheTtl, long usrCacheTtl, int cacheOffset, boolean requestGroups,
                         EndpointsConfiguration endpointsConfiguration, List<String> serviceAdminRoles, List<String> ignoreTenantRoles,
                         boolean sendAllTenantIds, boolean sendTenantIdQuality) {
        this.delegable = delegable;
        this.delegableQuality = delegableQuality;
        this.authServiceUri = authServiceUri;
        this.keyedRegexExtractor = keyedRegexExtractor;
        this.tenanted = tenanted;
        this.groupCacheTtl = groupCacheTtl;
        this.userCacheTtl = usrCacheTtl;
        this.tokenCacheTtl = tokenCacheTtl;
        this.cacheOffset = cacheOffset;
        this.requestGroups = requestGroups;
        this.endpointsConfiguration = endpointsConfiguration;
        this.serviceAdminRoles = serviceAdminRoles;
        this.ignoreTenantRoles = ignoreTenantRoles;
        this.sendAllTenantIds = sendAllTenantIds;
        this.sendTenantIdQuality = sendTenantIdQuality;
    }

    public boolean isDelegable() {
        return delegable;
    }

    public double getDelegableQuality() {
        return delegableQuality;
    }

    public String getAuthServiceUri() {
        return authServiceUri;
    }

    public KeyedRegexExtractor<String> getKeyedRegexExtractor() {
        return keyedRegexExtractor;
    }

    public boolean isTenanted() {
        return tenanted;
    }

    public long getGroupCacheTtl() {
        return groupCacheTtl;
    }

    public long getTokenCacheTtl() {
        return tokenCacheTtl;
    }

    public long getUserCacheTtl() {
        return userCacheTtl;
    }

    public boolean isRequestGroups() {
        return requestGroups;
    }

    public EndpointsConfiguration getEndpointsConfiguration() {
        return endpointsConfiguration;
    }

    public int getCacheOffset() {
        return cacheOffset;
    }

    public List<String> getServiceAdminRoles() {
        return serviceAdminRoles;
    }

    public List<String> getIgnoreTenantRoles() {
        return ignoreTenantRoles;
    }

    public boolean sendingAllTenantIds() {
        return sendAllTenantIds;
    }

    public boolean sendTenantIdQuality() {
        return sendTenantIdQuality;
    }
}
