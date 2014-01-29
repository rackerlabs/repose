package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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

        when(datastore.get(ACCOUNT)).thenReturn(new UserRateLimit(limitMap));

        assertFalse("Should return a non-empty set", rateLimitCache.getUserRateLimits(ACCOUNT).isEmpty());
    }

    @Test
    public void updateLimit_shouldSendPatchToDatastore() throws Exception {
        HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
        limitMap.put("testKey", new CachedRateLimit("foo"));
        when(datastore.patch(any(String.class), any(UserRateLimit.Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimitResult(limitMap, true));
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit();
        configuredRatelimit.setUnit(com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE);
        rateLimitCache.updateLimit(HttpMethod.GET, "bob", "testKey", configuredRatelimit, 5);
        verify(datastore).patch(eq("bob"), any(UserRateLimit.Patch.class), eq(1), eq(TimeUnit.MINUTES));
    }

    @Test
    public void updateLimit_usesReturnedValues_toPopulateResultObject() throws Exception {
        long now = System.currentTimeMillis();
        CachedRateLimit cachedRateLimit = new CachedRateLimit("foo");
        cachedRateLimit.logHit(HttpMethod.GET, com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE);
        HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
        limitMap.put("testKey", cachedRateLimit);
        when(datastore.patch(any(String.class), any(UserRateLimit.Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimitResult(limitMap, true));
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit();
        configuredRatelimit.setUnit(com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE);
        NextAvailableResponse response = rateLimitCache.updateLimit(HttpMethod.GET, "bob", "testKey", configuredRatelimit, 5);
        assertThat(response, hasValues(true, now, 1));
    }

    private Matcher<NextAvailableResponse> hasValues(final boolean hasRequests, final long resetTime, final int currentLimitAmount) {
        return new TypeSafeMatcher<NextAvailableResponse>() {
            @Override
            protected boolean matchesSafely(NextAvailableResponse item) {
                return (item.hasRequestsRemaining() == hasRequests) &&
                       (item.getResetTime().getTime() > resetTime) &&
                       (item.getResetTime().getTime() < (resetTime + 120000)) &&
                       (item.getCurrentLimitAmount() == currentLimitAmount);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Response with success: " + hasRequests + " reset time greater than: " + resetTime + "and less than: " + (resetTime + 120000) + " current limit amount: " + currentLimitAmount);
            }
        };
    }
}
