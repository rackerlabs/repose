package com.rackspace.papi.components.ratelimit.cache;

import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public interface RateLimitCache {

    Map<String, CachedRateLimit> getUserRateLimits(String key);

    NextAvailableResponse updateLimit(HttpMethod method, String account, String limitKey, ConfiguredRatelimit rateCfg) throws IOException;
}
