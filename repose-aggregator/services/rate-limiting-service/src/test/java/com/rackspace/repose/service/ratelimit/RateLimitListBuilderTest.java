package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author zinic
 */
public class RateLimitListBuilderTest extends RateLimitServiceTestContext {
    private Map<String, CachedRateLimit> cacheMap;
    private ConfiguredLimitGroup configuredLimitGroup;

    @Before
    public void standUp() {
        LinkedList<HttpMethod> methods = new LinkedList<HttpMethod>();
        methods.add(HttpMethod.GET);
        methods.add(HttpMethod.PUT);
        methods.add(HttpMethod.POST);
        methods.add(HttpMethod.DELETE);

        cacheMap = new HashMap<String, CachedRateLimit>();
        configuredLimitGroup = new ConfiguredLimitGroup();

        configuredLimitGroup.setDefault(Boolean.TRUE);
        configuredLimitGroup.setId("configured-limit-group");
        configuredLimitGroup.getGroups().add("user");

        cacheMap.put(SIMPLE_ID, new CachedRateLimit(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods, ""), 1));

        configuredLimitGroup.getLimit().add(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods, ""));

        cacheMap.put(COMPLEX_ID, new CachedRateLimit(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods, ""), 1));

        configuredLimitGroup.getLimit().add(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods, ""));
    }

    @Test
    public void shouldConstructLiveLimits() {
        final RateLimitList rll = new RateLimitListBuilder(cacheMap, configuredLimitGroup).toRateLimitList();

        final Limits limits = new Limits();
        limits.setRates(rll);

        assertEquals(2, rll.getRate().size());
    }
}
