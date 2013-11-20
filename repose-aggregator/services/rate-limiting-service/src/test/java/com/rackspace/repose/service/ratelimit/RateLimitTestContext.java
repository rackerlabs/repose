package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

public class RateLimitTestContext {
    public static final String SIMPLE_URI_REGEX = "/loadbalancer/.*", COMPLEX_URI_REGEX = "/loadbalancer/vips/.*",GROUPS_URI_REGEX = "/loadbalancer/(.*)/1234";
    public static final String SIMPLE_URI = "*loadbalancer*", COMPLEX_URI = "*loadbalancer/vips*", GROUPS_URI = "*loadbalancer/vips/cap1/1234*";


    public static ConfiguredRatelimit newLimitFor(String uri, String uriRegex, HttpMethod method) {
        final ConfiguredRatelimit configuredRateLimit = new ConfiguredRatelimit();

        configuredRateLimit.setUnit(TimeUnit.HOUR);
        configuredRateLimit.setUri(uri);
        configuredRateLimit.setUriRegex(uriRegex);
        configuredRateLimit.setValue(20);
        configuredRateLimit.getHttpMethods().add(method);

        return configuredRateLimit;
    }

    public static CachedRateLimit newCachedRateLimitFor(String uri, String uriRegex, HttpMethod... methods) {
        final CachedRateLimit cachedLimit = new CachedRateLimit(uriRegex);

        for (HttpMethod method : methods) {
            cachedLimit.logHit(method, TimeUnit.HOUR);
            cachedLimit.logHit(method, TimeUnit.HOUR);
        }

        return cachedLimit;
    }
}
