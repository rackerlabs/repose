/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.ratelimit.cache;

import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

// TODO: Still depend on Repose core

/**
 * @author jhopper
 */
public class ManagedRateLimitCacheTest {

    private String ACCOUNT = "12345";
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
        defaultConfig.setUnit(org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE);
        defaultConfig.getHttpMethods().add(HttpMethod.GET);
    }

    @Test
    public void getUserRateLimits_shouldReturnEmptySetsWhenNoLimitKeysExist() {
        assertThat("Should have an empty map when no limits have been registered for an account", rateLimitCache.getUserRateLimits("key").entrySet(), empty());
    }

    @Test
    public void getUserRateLimits_shouldReturnCachedKeySets() throws Exception {
        HashMap<String, CachedRateLimit> limitMap = new HashMap<>();
        limitMap.put("12345", new CachedRateLimit(defaultConfig));

        when(datastore.get(ACCOUNT)).thenReturn(new UserRateLimit(limitMap));

        assertThat("Should return a non-empty set", rateLimitCache.getUserRateLimits(ACCOUNT).entrySet(), not(empty()));
    }

    @Test
    public void updateLimit_shouldSendPatchToDatastore() throws Exception {
        HashMap<String, CachedRateLimit> limitMap = new HashMap<>();
        limitMap.put("testKey", new CachedRateLimit(defaultConfig));
        when(datastore.patch(any(String.class), any(UserRateLimit.Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimit(limitMap));
        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<>();
        matchingLimits.add(Pair.of("testKey", defaultConfig));
        rateLimitCache.updateLimit("bob", matchingLimits, org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 5);
        verify(datastore).patch(eq("bob"), any(UserRateLimit.Patch.class), eq(1), eq(TimeUnit.MINUTES));
    }

    @Test
    public void updateLimit_usesReturnedValues_toPopulateResultObject() throws Exception {
        long now = System.currentTimeMillis();
        CachedRateLimit cachedRateLimit = new CachedRateLimit(defaultConfig, 1);
        HashMap<String, CachedRateLimit> limitMap = new HashMap<>();
        limitMap.put("testKey", cachedRateLimit);
        UserRateLimit returnedLimit = spy(new UserRateLimit(limitMap));
        when(returnedLimit.getLowestLimit()).thenReturn(Pair.of(defaultConfig, cachedRateLimit));
        when(datastore.patch(any(String.class), any(UserRateLimit.Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(returnedLimit);
        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<>();
        matchingLimits.add(Pair.of("testKey", defaultConfig));
        NextAvailableResponse response = rateLimitCache.updateLimit("bob", matchingLimits, org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 5);
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
