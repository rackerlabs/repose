package org.openrepose.services.ratelimit;

import org.openrepose.services.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;

public final class RateLimitingServiceFactory {

   private RateLimitingServiceFactory() {
   }

   public static RateLimitingService createRateLimitingService(RateLimitCache cache, RateLimitingConfiguration rateLimitingConfiguration) {
      return new RateLimitingServiceImpl(cache, rateLimitingConfiguration);
   }
}
