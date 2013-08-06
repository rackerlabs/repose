package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.openstack.config.AdminRoles;

/**
 * @author fran
 *
 * This class manages information from config files related to auth.
 */
public class Configurables {
    private final boolean delegable;
    private final String authServiceUri;
    private final KeyedRegexExtractor<String> keyedRegexExtractor;
    private final boolean tenanted;
    private final long groupCacheTtl;
    private final long userCacheTtl;
    private final long tokenCacheTtl;
    private final boolean requestGroups;
    private final EndpointsConfiguration endpointsConfiguration;
    private final AdminRoles adminRoles;

    public Configurables(boolean delegable, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor,
                         boolean tenanted, long groupCacheTtl, long tokenCacheTtl, long usrCacheTtl, boolean requestGroups,
                         EndpointsConfiguration endpointsConfiguration) {
        this.delegable = delegable;
        this.authServiceUri = authServiceUri;
        this.keyedRegexExtractor = keyedRegexExtractor;
        this.tenanted = tenanted;
        this.groupCacheTtl = groupCacheTtl;
        this.userCacheTtl = usrCacheTtl;
        this.tokenCacheTtl = tokenCacheTtl;
        this.requestGroups=requestGroups;
        this.endpointsConfiguration = endpointsConfiguration;
        this.adminRoles = null;
    }

    public Configurables(boolean delegable, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor,
            boolean tenanted, long groupCacheTtl, long tokenCacheTtl, long usrCacheTtl, boolean requestGroups,
            EndpointsConfiguration endpointsConfiguration, AdminRoles adminRoles) {
        this.delegable = delegable;
        this.authServiceUri = authServiceUri;
        this.keyedRegexExtractor = keyedRegexExtractor;
        this.tenanted = tenanted;
        this.groupCacheTtl = groupCacheTtl;
        this.userCacheTtl = usrCacheTtl;
        this.tokenCacheTtl = tokenCacheTtl;
        this.requestGroups=requestGroups;
        this.endpointsConfiguration = endpointsConfiguration;
        this.adminRoles = adminRoles;
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

    public AdminRoles getAdminRoles() {
        return adminRoles;
    }
}
