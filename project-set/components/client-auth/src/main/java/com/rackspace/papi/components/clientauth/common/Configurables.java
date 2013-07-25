package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;

/**
 * @author fran
 */
public class Configurables {
    private final boolean delegable;
    private final String authServiceUri;
    private final KeyedRegexExtractor<String> keyedRegexExtractor;
    private final boolean tenanted;
    private final long groupCacheTtl;
    private final long userCacheTtl;
    private final boolean requestGroups;
	private final int cacheOffset;

    public Configurables(boolean delegable, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor, boolean tenanted, long groupCacheTtl, long tokenCacheTtl,int cacheOffset,boolean requestGroups) {
        this.delegable = delegable;
        this.authServiceUri = authServiceUri;
        this.keyedRegexExtractor = keyedRegexExtractor;
        this.tenanted = tenanted;
        this.groupCacheTtl = groupCacheTtl;
        this.userCacheTtl = tokenCacheTtl;
        this.cacheOffset = cacheOffset;
        this.requestGroups=requestGroups;
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

    public long getUserCacheTtl() {
        return userCacheTtl;
    }

    public int getCacheOffset() {
        return cacheOffset;
    }
    
    public boolean isRequestGroups() {
        return requestGroups;
    }
}
