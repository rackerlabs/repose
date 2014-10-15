package org.openrepose.services.ratelimit.cache;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.services.ratelimit.config.ConfiguredRatelimit;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NextAvailableResponseTest {
    private NextAvailableResponse nextAvailableResponse;
    private Pair<ConfiguredRatelimit, CachedRateLimit> limitPair;
    private long expirationTime;

    @Before
    public void setUp() throws Exception {
        ConfiguredRatelimit configLimit = mock(ConfiguredRatelimit.class);
        CachedRateLimit cachedLimit = mock(CachedRateLimit.class);

        expirationTime = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)).getTime();

        when(cachedLimit.amount()).thenReturn(5);
        when(cachedLimit.maxAmount()).thenReturn(10);
        when(cachedLimit.getNextExpirationTime()).thenReturn(expirationTime);

        limitPair = Pair.of(configLimit, cachedLimit);
        nextAvailableResponse = new NextAvailableResponse(limitPair);
    }

    @Test
    public void testGetResetTime() throws Exception {
        assertTrue(nextAvailableResponse.getResetTime().getTime() == expirationTime);
    }

    @Test
    public void testHasRequestsRemaining() throws Exception {
        assertTrue(nextAvailableResponse.hasRequestsRemaining());
    }

    @Test
    public void testGetCurrentLimitAmount() throws Exception {
        assertTrue(nextAvailableResponse.getCurrentLimitAmount() == 5);
    }

    @Test
    public void testGetLimitPair() throws Exception {
        assertTrue(nextAvailableResponse.getLimitPair() == limitPair);
    }
}
