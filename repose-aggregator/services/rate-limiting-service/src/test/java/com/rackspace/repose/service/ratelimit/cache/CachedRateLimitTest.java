package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class CachedRateLimitTest {

    public static class WhenLoggingHits {

        @Test
        public void shouldLogHits() {
            final CachedRateLimit limit = new CachedRateLimit("");
            limit.logHit(HttpMethod.GET, TimeUnit.HOUR);
            limit.logHit(HttpMethod.GET, TimeUnit.HOUR);
            limit.logHit(HttpMethod.GET, TimeUnit.HOUR);

            assertTrue(limit.amount(HttpMethod.GET) == 3);
        }

        @Test
        public void shouldGiveAccurateExpirationDates() {
            final CachedRateLimit limit = new CachedRateLimit("");
            limit.logHit(HttpMethod.GET, TimeUnit.MINUTE);
            limit.logHit(HttpMethod.GET, TimeUnit.HOUR);

            final long earliestExpiration = limit.getEarliestExpirationTime(HttpMethod.GET);
            final long latestExpiration = limit.getLatestExpirationTime(HttpMethod.GET);
            
            assertTrue(earliestExpiration < latestExpiration);
            assertTrue(earliestExpiration > System.currentTimeMillis());
            assertTrue(latestExpiration > System.currentTimeMillis());
        }

        @Test
        public void shouldVacuumExpiredHits() {
            final CachedRateLimit limit = new CachedRateLimit("");
            limit.logHit(HttpMethod.GET, System.currentTimeMillis());

            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
            }

            assertTrue(limit.amount(HttpMethod.GET) == 0);
        }

        @Test
        public void shouldMaintainHitsThatHaveNotExpired() {
            final CachedRateLimit limit = new CachedRateLimit("");
            limit.logHit(HttpMethod.GET, System.currentTimeMillis());

            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
            }

            limit.logHit(HttpMethod.GET, TimeUnit.HOUR);

            assertTrue(limit.amount(HttpMethod.GET) == 1);
        }
    }
}
