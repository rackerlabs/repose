package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author jhopper
 */
public class CachedRateLimitTest {
    private ConfiguredRatelimit cfg;

    @Before
    public void setup() {
        cfg = mock(ConfiguredRatelimit.class);

        when(cfg.getValue()).thenReturn(6);
        when(cfg.getUnit()).thenReturn(TimeUnit.MINUTE);
    }

    @Test
    public void shouldLogHits() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);
        limit.logHit();
        limit.logHit();
        limit.logHit();

        assertTrue(limit.amount() == 3);
    }

    @Test
    public void shouldGiveAccurateExpirationDates() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);
        limit.logHit();
        limit.logHit();

        final long soonestRequest = limit.getSoonestRequestTime();
        final long nextExpiration = limit.getNextExpirationTime();

        assertTrue(soonestRequest <= nextExpiration);
        assertTrue(soonestRequest >= System.currentTimeMillis());
        assertTrue(nextExpiration >= System.currentTimeMillis());
    }

    @Test
    public void shouldVacuumExpiredHits() {
        when(cfg.getValue()).thenReturn(3);
        when(cfg.getUnit()).thenReturn(TimeUnit.SECOND);

        final CachedRateLimit limit = new CachedRateLimit(cfg);
        limit.logHit();

        try {
            Thread.sleep(2);
        } catch (InterruptedException ie) {}

        assertTrue(limit.amount() == 0);
    }

    @Test
    public void shouldMaintainHitsThatHaveNotExpired() {
        when(cfg.getValue()).thenReturn(3);
        when(cfg.getUnit()).thenReturn(TimeUnit.SECOND);

        final CachedRateLimit limit = new CachedRateLimit(cfg);
        limit.logHit();

        try {
            Thread.sleep(2);
        } catch (InterruptedException ie) {}

        limit.logHit();

        assertTrue(limit.amount() == 1);
    }
}
