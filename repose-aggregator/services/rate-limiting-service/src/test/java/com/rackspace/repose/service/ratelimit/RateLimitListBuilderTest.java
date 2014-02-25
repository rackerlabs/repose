package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author zinic
 */
public class RateLimitListBuilderTest {
    public static final String SIMPLE_URI_REGEX = "/loadbalancer/.*", COMPLEX_URI_REGEX = "/loadbalancer/vips/.*", GROUPS_URI_REGEX = "/loadbalancer/(.*)/1234";
    public static final String SIMPLE_URI = "*loadbalancer*", COMPLEX_URI = "*loadbalancer/vips*", GROUPS_URI = "*loadbalancer/vips/cap1/1234*";
    public static final String SIMPLE_ID = "12345-ABCDE", COMPLEX_ID = "09876-ZYXWV";

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

        cacheMap.put(SIMPLE_ID, new CachedRateLimit(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods), 1));

        configuredLimitGroup.getLimit().add(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods));

        cacheMap.put(COMPLEX_ID, new CachedRateLimit(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods), 1));

        configuredLimitGroup.getLimit().add(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods));
    }

    @Test
    public void shouldConstructLiveLimits() {
        final RateLimitList rll = new RateLimitListBuilder(cacheMap, configuredLimitGroup).toRateLimitList();

        final Limits limits = new Limits();
        limits.setRates(rll);

        assertEquals(2, rll.getRate().size());
    }

    private ConfiguredRatelimit newLimitConfig(String limitId, String uri, String uriRegex, LinkedList<HttpMethod> methods) {
        final ConfiguredRatelimit configuredRateLimit = new ConfiguredRatelimit();

        configuredRateLimit.setId(limitId);
        configuredRateLimit.setUnit(TimeUnit.HOUR);
        configuredRateLimit.setUri(uri);
        configuredRateLimit.setUriRegex(uriRegex);
        configuredRateLimit.setValue(20);
        for (HttpMethod m : methods) {
            configuredRateLimit.getHttpMethods().add(m);
        }

        return configuredRateLimit;
    }
}
