package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author zinic
 */
public interface RateLimiterBuilder {

   RateLimiter buildRateLimiter(RateLimitCache cache, Map<String, Map<String, Pattern>> regexCache, RateLimitingConfiguration cfg);
}
