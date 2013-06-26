package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public interface RateLimitCache {

    Map<String, CachedRateLimit> getUserRateLimits(String key);

    NextAvailableResponse updateLimit(HttpMethod method, String account, String limitKey, ConfiguredRatelimit rateCfg, int datastoreWarnLimit) throws IOException;
}
