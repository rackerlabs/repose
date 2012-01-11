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

    public CachableTokenInfo cacheUserAuthToken(String accountUserId, int ttl, String userName, String authToken) {
        final CachableTokenInfo token = new CachableTokenInfo(userName, authToken);
        final Element newCacheElement = new Element(accountUserId, token);
        newCacheElement.setTimeToLive(ttl);

        authTokenCache.put(newCacheElement);
        return token;
    }
    
    public CachableTokenInfo tokenIsCached(String accountUserId, String headerAuthToken) {
        final Element element = authTokenCache.get(accountUserId);
        CachableTokenInfo token = element != null && !element.isExpired()? (CachableTokenInfo)element.getValue(): null;

        return token != null && token.getTokenId().equals(headerAuthToken)? token: null;
    }
}