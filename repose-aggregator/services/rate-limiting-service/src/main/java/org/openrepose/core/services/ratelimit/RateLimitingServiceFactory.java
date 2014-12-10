package org.openrepose.core.services.ratelimit;

import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration;
import org.openrepose.core.services.ratelimit.cache.RateLimitCache;

public final class RateLimitingServiceFactory {

   private RateLimitingServiceFactory() {
   }

   public static RateLimitingService createRateLimitingService(RateLimitCache cache, RateLimitingConfiguration rateLimitingConfiguration) {
      return new RateLimitingServiceImpl(cache, rateLimitingConfiguration);
   }
}
