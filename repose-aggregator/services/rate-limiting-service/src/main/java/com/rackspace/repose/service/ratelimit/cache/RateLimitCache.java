package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public interface RateLimitCache {

    Map<String, CachedRateLimit> getUserRateLimits(String key);

    NextAvailableResponse updateLimit(String account, List< Pair<String, ConfiguredRatelimit> > matchingLimits, TimeUnit largestUnit, int datastoreWarnLimit) throws IOException;
}
