package org.openrepose.services.ratelimit;

import org.apache.commons.lang3.tuple.Pair;
import org.openrepose.services.ratelimit.cache.NextAvailableResponse;
import org.openrepose.services.ratelimit.cache.RateLimitCache;
import org.openrepose.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.services.ratelimit.config.TimeUnit;
import org.openrepose.services.ratelimit.exception.CacheException;
import org.openrepose.services.ratelimit.exception.OverLimitException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/* Responsible for updating user rate limits and flagging if a user exceeds a limit */
public class RateLimiter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimiter.class);
    private final RateLimitCache cache;

    public RateLimiter(RateLimitCache cache) {
        this.cache = cache;
    }

    public void handleRateLimit(String user, List< Pair<String, ConfiguredRatelimit> > matchingLimits, TimeUnit largestUnit, int datastoreWarnLimit) throws OverLimitException {

        // Get the next, shortest available time that a user has to wait for
        try {
            NextAvailableResponse nextAvailable = cache.updateLimit(user, matchingLimits, largestUnit, datastoreWarnLimit);

            if (nextAvailable != null && !nextAvailable.hasRequestsRemaining()) {
                throw new OverLimitException("User rate limited!", user, nextAvailable.getResetTime(), nextAvailable.getCurrentLimitAmount(), nextAvailable.getLimitPair().getLeft().toString());
            }
        } catch (IOException ioe) {
            LOG.error("IOException caught during cache commit for rate limit user: " + user + " Reason: " + ioe.getMessage(), ioe);

            throw new CacheException("IOException caught during cache commit for rate limit.", ioe);
        }
    }
}
