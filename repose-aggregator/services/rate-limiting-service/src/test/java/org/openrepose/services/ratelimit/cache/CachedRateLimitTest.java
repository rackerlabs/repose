package org.openrepose.services.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.services.ratelimit.cache.CachedRateLimit;

import java.util.LinkedList;

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
        final LinkedList<HttpMethod> methods = new LinkedList<HttpMethod>();
        methods.add(HttpMethod.GET);
        methods.add(HttpMethod.POST);

        cfg = mock(ConfiguredRatelimit.class);

        when(cfg.getId()).thenReturn("12345-ABCDE");
        when(cfg.getUriRegex()).thenReturn(".*");
        when(cfg.getHttpMethods()).thenReturn(methods);
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
            Thread.sleep(2000);
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
            Thread.sleep(2000);
        } catch (InterruptedException ie) {}

        limit.logHit();

        assertTrue(limit.amount() == 1);
    }

    @Test
    public void getConfigLimitKey_shouldCreateCorrectCLKey() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertTrue(limit.getConfigId().equals(cfg.getId()));
    }

    @Test
    public void timestamp_get() {
        long before = System.currentTimeMillis();
        final CachedRateLimit limit = new CachedRateLimit(cfg);
        long after = System.currentTimeMillis();

        assertTrue(limit.timestamp() >= before);
        assertTrue(limit.timestamp() <= after);
    }

    @Test
    public void amount_get() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertTrue(limit.amount() == 0);

        limit.logHit();

        assertTrue(limit.amount() == 1);
    }

    @Test
    public void unit_get() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertTrue(limit.unit() == java.util.concurrent.TimeUnit.MINUTES.toMillis(1));
    }

    @Test
    public void maxAmount_get() {
        final CachedRateLimit limit = new CachedRateLimit(cfg);

        assertTrue(limit.maxAmount() == 6);
    }
}
