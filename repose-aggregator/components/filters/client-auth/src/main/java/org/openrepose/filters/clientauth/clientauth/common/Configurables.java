package org.openrepose.filters.clientauth.clientauth.common;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 *         <p/>
 *         This class manages information from config files related to auth.
 */
public class Configurables {
    private final boolean delegable;
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

    public Configurables(boolean delegable, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor,
                         boolean tenanted, long groupCacheTtl, long tokenCacheTtl, long usrCacheTtl, boolean requestGroups,
                         EndpointsConfiguration endpointsConfiguration, boolean sendAllTenantIds) {
        this.delegable = delegable;
        this.authServiceUri = authServiceUri;
        this.keyedRegexExtractor = keyedRegexExtractor;
        this.tenanted = tenanted;
        this.groupCacheTtl = groupCacheTtl;
        this.userCacheTtl = usrCacheTtl;
        this.tokenCacheTtl = tokenCacheTtl;
        this.requestGroups = requestGroups;
        this.endpointsConfiguration = endpointsConfiguration;
        this.serviceAdminRoles = new ArrayList<String>();
        this.ignoreTenantRoles = new ArrayList<String>();
        this.cacheOffset = 0;
        this.sendAllTenantIds = sendAllTenantIds;
    }

    public Configurables(boolean delegable, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor,
                         boolean tenanted, long groupCacheTtl, long tokenCacheTtl, long usrCacheTtl, int cacheOffset, boolean requestGroups,
                         EndpointsConfiguration endpointsConfiguration, List<String> serviceAdminRoles, List<String> ignoreTenantRoles,
                         boolean sendAllTenantIds) {
        this.delegable = delegable;
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
    }

    public boolean isDelegable() {
        return delegable;
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

    public boolean sendingAllTenantIds() { return sendAllTenantIds; }
}
