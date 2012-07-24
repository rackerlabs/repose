package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;

public final class RateLimitingServiceFactory {

   private RateLimitingServiceFactory() {
   }

   public static RateLimitingService createRateLimitingService(RateLimitCache cache, RateLimitingConfiguration rateLimitingConfiguration) {
      return new RateLimitingServiceImpl(cache, rateLimitingConfiguration);
   }
}
