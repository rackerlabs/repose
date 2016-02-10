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
package org.openrepose.core.services.ratelimit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.cache.NextAvailableResponse;
import org.openrepose.core.services.ratelimit.cache.RateLimitCache;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.TimeUnit;
import org.openrepose.core.services.ratelimit.exception.CacheException;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;

public class RateLimiterTest {

    private static final String USER = "a user";
    private static final String URI = "/some/uri/";
    private final Pattern pattern = Pattern.compile("/some/uri/(.*)");
    private final Matcher uriMatcher = pattern.matcher(URI);
    private final RateLimitCache mockedCache = mock(RateLimitCache.class);
    private final ConfiguredRatelimit configuredRateLimit = RateLimitingTestSupport.defaultRateLimitingConfiguration().getLimitGroup().get(0).getLimit().get(0);
    private String key;
    private int datastoreWarnLimit = 1000;

    @Before
    public void setup() {
        uriMatcher.matches();
        key = LimitKey.getLimitKey("unique-group", configuredRateLimit.getId(), uriMatcher, true);
    }

    @Test(expected = OverLimitException.class)
    public void shouldThrowOverLimitException() throws IOException, OverLimitException {
        final RateLimiter rateLimiter = new RateLimiter(mockedCache);

        when(mockedCache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(
                Pair.of(configuredRateLimit, new CachedRateLimit(configuredRateLimit, 10))));

        ArrayList<Pair<String, ConfiguredRatelimit>> limitMap = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        limitMap.add(Pair.of(key, configuredRateLimit));

        rateLimiter.handleRateLimit(USER, limitMap, configuredRateLimit.getUnit(), datastoreWarnLimit);
    }

    @Test(expected = CacheException.class)
    public void shouldThrowCacheException() throws OverLimitException, IOException {
        final RateLimiter rateLimiter = new RateLimiter(mockedCache);

        when(mockedCache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenThrow(new IOException("uh oh"));

        ArrayList<Pair<String, ConfiguredRatelimit>> limitMap = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        limitMap.add(Pair.of(key, configuredRateLimit));

        rateLimiter.handleRateLimit(USER, limitMap, configuredRateLimit.getUnit(), datastoreWarnLimit);
    }

    @Test
    public void shouldUpdateLimitWithoutExceptions() throws IOException, OverLimitException {
        final RateLimiter rateLimiter = new RateLimiter(mockedCache);

        when(mockedCache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(
                Pair.of(configuredRateLimit, new CachedRateLimit(configuredRateLimit, 1))));

        ArrayList<Pair<String, ConfiguredRatelimit>> limitMap = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        limitMap.add(Pair.of(key, configuredRateLimit));

        rateLimiter.handleRateLimit(USER, limitMap, configuredRateLimit.getUnit(), datastoreWarnLimit);
    }
}
