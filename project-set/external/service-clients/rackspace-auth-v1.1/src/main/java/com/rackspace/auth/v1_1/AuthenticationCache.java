package com.rackspace.auth.v1_1;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * @author fran
 *
 *
 */
public class AuthenticationCache {
    private final TokenCache authTokenCache;

    public AuthenticationCache(CacheManager cache) {
        this.authTokenCache = new TokenCache(cache);
    }

    public void cacheUserAuthToken(String accountUsername, int ttl, String authToken) {
        final Element newCacheElement = new Element(accountUsername, authToken);
        newCacheElement.setTimeToLive(ttl);

        authTokenCache.put(newCacheElement);
    }

    public boolean tokenIsCached(String accountUsername, String headerAuthToken) {
        final Element cachedTokenElement = authTokenCache.get(accountUsername);

        return cachedTokenElement != null && !cachedTokenElement.isExpired() &&
               cachedTokenElement.getValue().equals(headerAuthToken);
    }
}