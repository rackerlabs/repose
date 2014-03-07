package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.cache.NextAvailableResponse;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.repose.service.ratelimit.exception.CacheException;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

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

        ArrayList< Pair<String, ConfiguredRatelimit> > limitMap = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        limitMap.add(Pair.of(key, configuredRateLimit));

        rateLimiter.handleRateLimit(USER, limitMap, configuredRateLimit.getUnit(), datastoreWarnLimit);
    }

    @Test(expected = CacheException.class)
    public void shouldThrowCacheException() throws OverLimitException, IOException {
        final RateLimiter rateLimiter = new RateLimiter(mockedCache);

        when(mockedCache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenThrow(new IOException("uh oh"));

        ArrayList< Pair<String, ConfiguredRatelimit> > limitMap = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        limitMap.add(Pair.of(key, configuredRateLimit));

        rateLimiter.handleRateLimit(USER, limitMap, configuredRateLimit.getUnit(), datastoreWarnLimit);
    }

    @Test
    public void shouldUpdateLimitWithoutExceptions() throws IOException, OverLimitException {
        final RateLimiter rateLimiter = new RateLimiter(mockedCache);

        when(mockedCache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(
                Pair.of(configuredRateLimit, new CachedRateLimit(configuredRateLimit, 1))));

        ArrayList< Pair<String, ConfiguredRatelimit> > limitMap = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        limitMap.add(Pair.of(key, configuredRateLimit));

        rateLimiter.handleRateLimit(USER, limitMap, configuredRateLimit.getUnit(), datastoreWarnLimit);
    }
}
