package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.settings.RateLimitingSettings;

public class RateLimitingServiceFactory {

   private RateLimitingServiceFactory() {
   }

   public static RateLimitingService createRateLimitingService(RateLimitCache cache, RateLimitingSettings settings) {
      return new RateLimitingServiceImpl(cache, settings);   
   }
}
