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
package org.openrepose.core.services.ratelimit.cache

import org.apache.commons.lang3.tuple.Pair
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert
import org.junit.Test
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit
import org.openrepose.core.services.ratelimit.config.HttpMethod

import java.util.concurrent.TimeUnit

import static org.hamcrest.Matchers.sameInstance
import static org.hamcrest.Matchers.not
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/28/14
 * Time: 3:48 PM
 */
class UserRateLimitTest {

    @Test
    void "patch should apply change when within limit"() {
        String limitId = "testId"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(limitId, uriRegex, [method], org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(configuredRatelimit)
        startingLimitMap.put(limitId, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)
        long now = System.currentTimeMillis()

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitId, configuredRatelimit));

        UserRateLimit patchedLimit = new UserRateLimit.Patch(matchingLimits).applyPatch(rateLimit)
        assertThat(patchedLimit, containsLimit(limitId, [method], now, TimeUnit.MINUTES, uriRegex))
    }

    @Test
    void "patch should apply change when over limit then return without applying further changes"() {
        String limitId = "testId"
        String limitId2 = "testId2"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(limitId, uriRegex, [method], org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 0)
        ConfiguredRatelimit configuredRatelimit2 = validConfiguredRateLimit(limitId2, uriRegex, [HttpMethod.ALL], org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 0)

        UserRateLimit rateLimit = new UserRateLimit()

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitId, configuredRatelimit));
        matchingLimits.add(Pair.of(limitId2, configuredRatelimit2));

        UserRateLimit patchedLimit = new UserRateLimit.Patch(matchingLimits).applyPatch(rateLimit)
        Assert.assertEquals(1, patchedLimit.limitMap.get(limitId).amount())
        assertEquals(null, patchedLimit.limitMap.get(limitId2))
    }

    @Test
    void "patch should work when the limit key isn't yet in map"() {
        String limitId = "testId"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(limitId, uriRegex, [method], org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 1)

        UserRateLimit rateLimit = new UserRateLimit()

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitId, configuredRatelimit));

        UserRateLimit patchedLimit = new UserRateLimit.Patch(matchingLimits).applyPatch(rateLimit)
        assertThat(patchedLimit, containsLimit(limitId, [method], System.currentTimeMillis(), TimeUnit.MINUTES, uriRegex))
    }

    @Test
    void "patch should be returning a different object than the one in the datastore"() {
        String limitId = "testId"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(limitId, uriRegex, [method], org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(configuredRatelimit)
        startingLimitMap.put(limitId, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitId, configuredRatelimit));

        UserRateLimit patchedLimit = new UserRateLimit.Patch(matchingLimits).applyPatch(rateLimit)
        assertThat(patchedLimit, not(sameInstance(rateLimit)))
    }

    @Test
    void "newFromPatch should create a correct rate limit when within limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.id = "12345-ABCDE"
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        configuredRatelimit.getHttpMethods().add(HttpMethod.GET)

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of("12345-ABCDE", configuredRatelimit));

        UserRateLimit.Patch patch = new UserRateLimit.Patch(matchingLimits)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit, containsLimit("12345-ABCDE", [HttpMethod.GET], System.currentTimeMillis(), TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit with multiple http methods when within limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.id = "12345-ABCDE"
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        configuredRatelimit.getHttpMethods().addAll([HttpMethod.GET, HttpMethod.POST])

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of("12345-ABCDE", configuredRatelimit));

        UserRateLimit.Patch patch = new UserRateLimit.Patch(matchingLimits)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit, containsLimit("12345-ABCDE", [HttpMethod.GET, HttpMethod.POST], System.currentTimeMillis(), TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit when outside limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.id = "12345-ABCDE"
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE
        configuredRatelimit.value = 0
        configuredRatelimit.getHttpMethods().add(HttpMethod.GET)

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of("12345-ABCDE", configuredRatelimit));

        UserRateLimit.Patch patch = new UserRateLimit.Patch(matchingLimits)
        UserRateLimit userRateLimit = patch.newFromPatch()
        Assert.assertEquals(userRateLimit.limitMap.get("12345-ABCDE").amount(), 1)
    }

    private ConfiguredRatelimit validConfiguredRateLimit(String limitId, String uriRegex, List<HttpMethod> methods, org.openrepose.core.services.ratelimit.config.TimeUnit unit, int value) {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.id = limitId
        configuredRatelimit.uriRegex = uriRegex
        configuredRatelimit.httpMethods = methods
        configuredRatelimit.unit = unit
        configuredRatelimit.value = value
        configuredRatelimit
    }

    static Matcher<UserRateLimit> containsLimit(String limitId, List<HttpMethod> methods, long startTime, TimeUnit timeUnit, String uriRegex) {
        return new TypeSafeMatcher<UserRateLimit>() {
            @Override
            protected boolean matchesSafely(UserRateLimit item) {
                Map<String, CachedRateLimit> limitMap = item.getLimitMap()
                boolean containsKey = limitMap.containsKey(limitId)
                boolean containsConfigLimitKey = limitMap.get(limitId).configId ==
                        limitId
                boolean expiration = limitMap.get(limitId).getNextExpirationTime() >= (startTime) &&
                        limitMap.get(limitId).getNextExpirationTime() <= (startTime + timeUnit.toMillis(1))
                return containsKey &&
                        containsConfigLimitKey &&
                        expiration;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("An UserRateLimit with a limit key:" + limitId + " methods: " + methods + " a time stamp near:" + startTime + " plus 1 " + timeUnit)
            }
        };
    }
}
