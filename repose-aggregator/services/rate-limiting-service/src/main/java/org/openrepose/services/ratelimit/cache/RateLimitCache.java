package org.openrepose.services.ratelimit.cache;

import org.apache.commons.lang3.tuple.Pair;
import org.openrepose.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.services.ratelimit.config.TimeUnit;

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
