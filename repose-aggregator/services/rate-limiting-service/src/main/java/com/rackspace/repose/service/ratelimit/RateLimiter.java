package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.NextAvailableResponse;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.repose.service.ratelimit.exception.CacheException;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import org.apache.commons.lang3.tuple.Pair;
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
