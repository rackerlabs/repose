package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: Still depend on Repose core

/**
 *
 * @author jhopper
 */
public class ManagedRateLimitCacheTest {

    private String ACCOUNT = "12345";
    private int datastoreWarnLimit= 1000;
    private Datastore datastore;
    private ManagedRateLimitCache rateLimitCache;

    @Before
    public void setUp() throws Exception {
        datastore = mock(Datastore.class);
        rateLimitCache = new ManagedRateLimitCache(datastore);
    }

    @Test
    public void getUserRateLimits_shouldReturnEmptySetsWhenNoLimitKeysExist() {
        assertTrue("Should have an empty map when no limits have been registered for an account", rateLimitCache.getUserRateLimits("key").isEmpty());
    }

    @Test
    public void getUserRateLimits_shouldReturnCachedKeySets() throws Exception {
        HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
        limitMap.put("12345", new CachedRateLimit(".*"));

        when(datastore.get(ACCOUNT)).thenReturn(limitMap);

        assertFalse("Should return a non-empty set", rateLimitCache.getUserRateLimits(ACCOUNT).isEmpty());
    }

    @Test
    public void updateLimit_shouldAddNewLimit() throws Exception {
        final String user = "12345", key = "variant";
        final ConfiguredRatelimit rate = new ConfiguredRatelimit();
        rate.setUriRegex(".*");
        rate.setUnit(TimeUnit.HOUR);
        rate.setValue(3);

        final HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();

        when(datastore.get(user)).thenReturn(limitMap);

        rateLimitCache.updateLimit(HttpMethod.GET, user, key, rate, datastoreWarnLimit);

        verify(datastore).put(eq(user), any(Serializable.class), eq(1), eq(java.util.concurrent.TimeUnit.HOURS));
    }

    @Test
    public void updateLimit_shouldReturnValidNextAvailableResponse() throws Exception {
        final String account = "12345", key = "variant";
        final ConfiguredRatelimit rate = new ConfiguredRatelimit();
        rate.setUriRegex(".*");
        rate.setValue(2);
        rate.setUnit(TimeUnit.HOUR);

        final HashMap<String, CachedRateLimit> liveLimitMap = new HashMap<String, CachedRateLimit>();
        liveLimitMap.put(key, new CachedRateLimit(".*"));

        when(datastore.get(account)).thenReturn(liveLimitMap);

        assertTrue(rateLimitCache.updateLimit(HttpMethod.GET, account, key, rate, datastoreWarnLimit).hasRequestsRemaining());
        assertTrue(rateLimitCache.updateLimit(HttpMethod.GET, account, key, rate, datastoreWarnLimit).hasRequestsRemaining());
        assertFalse(rateLimitCache.updateLimit(HttpMethod.GET, account, key, rate, datastoreWarnLimit).hasRequestsRemaining());
    }
}
