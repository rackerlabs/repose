package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author zinic
 */
public final class DefaultRateLimiterBuilder implements RateLimiterBuilder {

   private static final DefaultRateLimiterBuilder INSTANCE = new DefaultRateLimiterBuilder();

   public static RateLimiterBuilder getInstance() {
      return INSTANCE;
   }
   
   private DefaultRateLimiterBuilder() {
   }

   @Override
   public RateLimiter buildRateLimiter(RateLimitCache cache, Map<String, Map<String, Pattern>> regexCache, RateLimitingConfiguration cfg) {
      return new RateLimiter(cache, regexCache, cfg);
   }
}
