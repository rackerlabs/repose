package org.openrepose.services.ratelimit.cache;

import org.openrepose.services.datastore.Datastore;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
    private ConfiguredRatelimit defaultConfig = new ConfiguredRatelimit();

    @Before
    public void setUp() throws Exception {
        datastore = mock(Datastore.class);
        rateLimitCache = new ManagedRateLimitCache(datastore);

        defaultConfig.setUri(".*");
        defaultConfig.setUriRegex(".*");
        defaultConfig.setValue(2);
        defaultConfig.setUnit(com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE);
        defaultConfig.getHttpMethods().add(HttpMethod.GET);
    }

    @Test
    public void getUserRateLimits_shouldReturnEmptySetsWhenNoLimitKeysExist() {
        assertTrue("Should have an empty map when no limits have been registered for an account", rateLimitCache.getUserRateLimits("key").isEmpty());
    }

    @Test
    public void getUserRateLimits_shouldReturnCachedKeySets() throws Exception {
        HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
        limitMap.put("12345", new CachedRateLimit(defaultConfig));

        when(datastore.get(ACCOUNT)).thenReturn(new UserRateLimit(limitMap));

        assertFalse("Should return a non-empty set", rateLimitCache.getUserRateLimits(ACCOUNT).isEmpty());
    }

    @Test
    public void updateLimit_shouldSendPatchToDatastore() throws Exception {
        HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
        limitMap.put("testKey", new CachedRateLimit(defaultConfig));
        when(datastore.patch(any(String.class), any(UserRateLimit.Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimit(limitMap));
        ArrayList< Pair<String, ConfiguredRatelimit> > matchingLimits = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        matchingLimits.add(Pair.of("testKey", defaultConfig));
        rateLimitCache.updateLimit("bob", matchingLimits, com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 5);
        verify(datastore).patch(eq("bob"), any(UserRateLimit.Patch.class), eq(1), eq(TimeUnit.MINUTES));
    }

    @Test
    public void updateLimit_usesReturnedValues_toPopulateResultObject() throws Exception {
        long now = System.currentTimeMillis();
        CachedRateLimit cachedRateLimit = new CachedRateLimit(defaultConfig);
        cachedRateLimit.logHit();
        HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
        limitMap.put("testKey", cachedRateLimit);
        UserRateLimit returnedLimit = spy(new UserRateLimit(limitMap));
        when(returnedLimit.getLowestLimit()).thenReturn(Pair.of(defaultConfig, cachedRateLimit));
        when(datastore.patch(any(String.class), any(UserRateLimit.Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(returnedLimit);
        ArrayList< Pair<String, ConfiguredRatelimit> > matchingLimits = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        matchingLimits.add(Pair.of("testKey", defaultConfig));
        NextAvailableResponse response = rateLimitCache.updateLimit("bob", matchingLimits, com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 5);
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
