package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.ratelimit.cache.util.ObjectSerializer;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

// TODO: Still depend on Repose core
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class ManagedRateLimitCacheTest {

    public static final String ACCOUNT = "12345";
    public static final int datastoreWarnLimit= 1000;


    public static class WhenRetrievingAccountLimitKeys {

        @Test
        public void shouldReturnEmptySetsWhenNoLimitKeysExist() {
            final Datastore cacheMock = mock(Datastore.class);
            when(cacheMock.get(any(String.class))).thenReturn(new StoredElementImpl("key", null));
            
            final ManagedRateLimitCache cache = new ManagedRateLimitCache(cacheMock);
            assertTrue("Should have an empty map when no limits have been registered for an account", cache.getUserRateLimits("key").isEmpty());
        }

        @Test
        public void shouldReturnCachedKeySets() throws Exception {
            final Datastore cacheMock = mock(Datastore.class);
            final HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
            limitMap.put("12345", new CachedRateLimit(".*"));
            
            when(cacheMock.get(ACCOUNT)).thenReturn(new StoredElementImpl(ACCOUNT, ObjectSerializer.instance().writeObject(limitMap)));
            
            final ManagedRateLimitCache cache = new ManagedRateLimitCache(cacheMock);
            assertFalse("Should return a non-empty set", cache.getUserRateLimits(ACCOUNT).isEmpty());
        }
    }

    public static class WhenUpdatingRateLimits {

        private ManagedRateLimitCache cache;

        @Test
        public void shouldAddNewLimit() throws Exception {
            final String user = "12345", key = "variant";
            final ConfiguredRatelimit rate = new ConfiguredRatelimit();
            rate.setUriRegex(".*");
            rate.setUnit(TimeUnit.HOUR);
            rate.setValue(3);
            
            final HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
            
            final Datastore cacheMock = mock(Datastore.class);
            when(cacheMock.get(user)).thenReturn(new StoredElementImpl(key, ObjectSerializer.instance().writeObject(limitMap)));
            
            cache = new ManagedRateLimitCache(cacheMock);

            cache.updateLimit(HttpMethod.GET, user, key, rate, datastoreWarnLimit);
            
            verify(cacheMock).put(eq(user), any(byte[].class), eq(1), eq(java.util.concurrent.TimeUnit.HOURS));
        }

        @Test
        public void shouldReturnValidNextAvailableResponse() throws Exception {
            final String account = "12345", key = "variant";
            final ConfiguredRatelimit rate = new ConfiguredRatelimit();
            rate.setUriRegex(".*");
            rate.setValue(2);
            rate.setUnit(TimeUnit.HOUR);
         
            final HashMap<String, CachedRateLimit> liveLimitMap = new HashMap<String, CachedRateLimit>();
            liveLimitMap.put(key, new CachedRateLimit(".*"));

            final Datastore cacheMock = mock(Datastore.class);
            when(cacheMock.get(account)).thenReturn(new StoredElementImpl(key, ObjectSerializer.instance().writeObject(liveLimitMap)));
            
            cache = new ManagedRateLimitCache(cacheMock);

            assertTrue(cache.updateLimit(HttpMethod.GET, account, key, rate, datastoreWarnLimit).hasRequestsRemaining());
            assertTrue(cache.updateLimit(HttpMethod.GET, account, key, rate, datastoreWarnLimit).hasRequestsRemaining());
            assertFalse(cache.updateLimit(HttpMethod.GET, account, key, rate, datastoreWarnLimit).hasRequestsRemaining());
        }
    }
}
