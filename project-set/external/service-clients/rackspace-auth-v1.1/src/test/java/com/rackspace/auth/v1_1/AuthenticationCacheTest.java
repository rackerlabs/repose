package com.rackspace.auth.v1_1;

import net.sf.ehcache.CacheManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class AuthenticationCacheTest {
    public static class WhenUsingAuthenticationCache {
        private CacheManager cacheManager;
        private AuthenticationCache authenticationCache;

        @Before
        public void setup() {
            cacheManager = new CacheManager();
            authenticationCache = new AuthenticationCache(cacheManager);
        }

        @Test
        public void shouldCacheAuthToken() {
            String userId = "1234";
            String username = "username";
            int ttl = 60000;
            String authToken = "1234567";

            authenticationCache.cacheUserAuthToken(userId, ttl, username, authToken);

            assertTrue(authenticationCache.tokenIsCached(userId, authToken) != null);
        }        
    }
}
