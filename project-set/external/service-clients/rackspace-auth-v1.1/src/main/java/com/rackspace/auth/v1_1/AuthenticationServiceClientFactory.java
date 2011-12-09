package com.rackspace.auth.v1_1;

import net.sf.ehcache.CacheManager;

/**
 * @author fran
 */
public class AuthenticationServiceClientFactory {
    public AuthenticationServiceClient buildAuthServiceClient(String targetHostUri, String username, String password) {
        
        return new AuthenticationServiceClient(targetHostUri, new ResponseUnmarshaller(), new ServiceClient(username, password),
                                               new CacheManager(), new AuthenticationCache(new CacheManager()));
    }
}
