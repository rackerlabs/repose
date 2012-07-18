package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;

import com.rackspace.repose.service.ratelimit.cache.NextAvailableResponse;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

import com.rackspace.repose.service.ratelimit.exception.CacheException;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import org.slf4j.Logger;

import java.io.IOException;

public class RateLimiter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimiter.class);
   private final RateLimitCache cache;

   public RateLimiter(RateLimitCache cache) {
      this.cache = cache;
   }

   public void handleRateLimit(String user, String limitKey, ConfiguredRatelimit rateLimit) throws OverLimitException {

      // Get the next, shortest available time that a user has to wait for
      try {
         // update the cache for each method in single rate limit
         NextAvailableResponse nextAvailable = null;
         for (HttpMethod configuredMethod : rateLimit.getHttpMethods()) {
            nextAvailable = cache.updateLimit(configuredMethod, user, limitKey, rateLimit);
         }

         if (nextAvailable != null && !nextAvailable.hasRequestsRemaining()) {
            throw new OverLimitException("User rate limited!", user, nextAvailable.getResetTime(), nextAvailable.getCurrentLimitAmount(), rateLimit.toString());
         }
      } catch (IOException ioe) {
         LOG.error("IOException caught during cache commit for rate limit user: " + user + " Reason: " + ioe.getMessage(), ioe);

         throw new CacheException("IOException caught during cache commit for rate limit.", ioe);
      }
   }
}
