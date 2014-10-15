package org.openrepose.services.ratelimit;

import org.openrepose.services.ratelimit.cache.RateLimitCache;
import org.openrepose.services.ratelimit.config.RateLimitingConfiguration;

public final class RateLimitingServiceFactory {

   private RateLimitingServiceFactory() {
   }

   public static RateLimitingService createRateLimitingService(RateLimitCache cache, RateLimitingConfiguration rateLimitingConfiguration) {
      return new RateLimitingServiceImpl(cache, rateLimitingConfiguration);
   }
}
