/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.ratelimit;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.config.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
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

        cacheMap = new HashMap<>();
        configuredLimitGroup = new ConfiguredLimitGroup();

        configuredLimitGroup.setDefault(Boolean.TRUE);
        configuredLimitGroup.setId("configured-limit-group");
        configuredLimitGroup.getGroups().add("user");

        cacheMap.put(SIMPLE_ID, new CachedRateLimit(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods, new LinkedList<>()), 1));
        configuredLimitGroup.getLimit().add(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods, new LinkedList<>()));

        cacheMap.put(COMPLEX_ID, new CachedRateLimit(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods, new LinkedList<>()), 1));
        configuredLimitGroup.getLimit().add(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods, new LinkedList<>()));

        cacheMap.put("methods-test-empty", new CachedRateLimit(newLimitConfig("methods-test-empty", "/methods/empty/*", "/methods/empty/.*", Collections.emptyList(), new LinkedList<>()), 5));
        configuredLimitGroup.getLimit().add(newLimitConfig("methods-test-empty", "/methods/empty/*", "/methods/empty/.*", Collections.emptyList(), new LinkedList<>()));

        cacheMap.put("methods-test-all", new CachedRateLimit(newLimitConfig("methods-test-all", "/methods/all/*", "/methods/all/.*", Collections.singletonList(HttpMethod.ALL), new LinkedList<>()), 5));
        configuredLimitGroup.getLimit().add(newLimitConfig("methods-test-all", "/methods/all/*", "/methods/all/.*", Collections.singletonList(HttpMethod.ALL), new LinkedList<>()));
    }

    @Test
    public void shouldConstructLiveLimits() {
        final RateLimitList rll = new RateLimitListBuilder(cacheMap, configuredLimitGroup).toRateLimitList();

        final Limits limits = new Limits();
        limits.setRates(rll);

        assertEquals(4, rll.getRate().size());
        for (ResourceRateLimits resourceRateLimit : rll.getRate()) {
            assertThat(resourceRateLimit.getLimit().size(),
                Matchers.either(Matchers.is(4)).or(Matchers.is(1)));

            for (RateLimit rateLimit : resourceRateLimit.getLimit()) {
                assertThat(rateLimit.getRemaining(),
                    Matchers.either(Matchers.is(19)).or(Matchers.is(15)));
            }
        }
    }
}
