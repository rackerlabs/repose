package com.rackspace.repose.service.ratelimit.cache
import com.rackspace.repose.service.limits.schema.HttpMethod
import com.rackspace.repose.service.ratelimit.LimitKey
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit
import org.apache.commons.lang3.tuple.Pair
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Test

import java.util.concurrent.TimeUnit

import static org.hamcrest.CoreMatchers.sameInstance
import static org.hamcrest.core.IsNot.not
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
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(configuredRatelimit)
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)
        long now = System.currentTimeMillis()

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitKey, configuredRatelimit));

        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(matchingLimits))
        assertThat(patchedLimit, containsLimit(limitKey, [method], now, TimeUnit.MINUTES, uriRegex))
    }

    @Test
    void "patch should apply change when over limit then return without applying further changes"() {
        String limitKey = "testKey"
        String limitKey2 = "testKey2"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 0)
        ConfiguredRatelimit configuredRatelimit2 = validConfiguredRateLimit(uriRegex, [HttpMethod.ALL], com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 0)

        UserRateLimit rateLimit = new UserRateLimit()

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitKey, configuredRatelimit));
        matchingLimits.add(Pair.of(limitKey2, configuredRatelimit2));

        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(matchingLimits))
        assertEquals(1, patchedLimit.limitMap.get(limitKey).amount())
        assertEquals(null, patchedLimit.limitMap.get(limitKey2))
    }

    @Test
    void "patch should work when the limit key isn't yet in map"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 1)

        UserRateLimit rateLimit = new UserRateLimit()

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitKey, configuredRatelimit));

        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(matchingLimits))
        assertThat(patchedLimit, containsLimit(limitKey, [method], System.currentTimeMillis(), TimeUnit.MINUTES, uriRegex))
    }

    @Test
    void "patch should be returning a different object than the one in the datastore"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(configuredRatelimit)
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of(limitKey, configuredRatelimit));

        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(matchingLimits))
        assertThat(patchedLimit, not(sameInstance(rateLimit)))
    }

    @Test
    void "newFromPatch should create a correct rate limit when within limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        configuredRatelimit.getHttpMethods().add(HttpMethod.GET)

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of("testKey", configuredRatelimit));

        UserRateLimit.Patch patch = new UserRateLimit.Patch(matchingLimits)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit, containsLimit("testKey", [HttpMethod.GET], System.currentTimeMillis(), TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit with multiple http methods when within limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        configuredRatelimit.getHttpMethods().addAll([HttpMethod.GET, HttpMethod.POST])

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of("testKey", configuredRatelimit));

        UserRateLimit.Patch patch = new UserRateLimit.Patch(matchingLimits)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit, containsLimit("testKey", [HttpMethod.GET, HttpMethod.POST], System.currentTimeMillis(), TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit when outside limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE
        configuredRatelimit.value = 0
        configuredRatelimit.getHttpMethods().add(HttpMethod.GET)

        ArrayList<Pair<String, ConfiguredRatelimit>> matchingLimits = new ArrayList<Pair<String, ConfiguredRatelimit>>();
        matchingLimits.add(Pair.of("testKey", configuredRatelimit));

        UserRateLimit.Patch patch = new UserRateLimit.Patch(matchingLimits)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertEquals(userRateLimit.limitMap.get("testKey").amount(), 1)
    }

    private ConfiguredRatelimit validConfiguredRateLimit(String uriRegex, List<HttpMethod> methods, com.rackspace.repose.service.limits.schema.TimeUnit unit, int value) {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = uriRegex
        configuredRatelimit.httpMethods = methods
        configuredRatelimit.unit = unit
        configuredRatelimit.value = value
        configuredRatelimit
    }

    static Matcher<UserRateLimit> containsLimit(String limitKey, List<HttpMethod> methods, long startTime, TimeUnit timeUnit, String uriRegex) {
        return new TypeSafeMatcher<UserRateLimit>() {
            @Override
            protected boolean matchesSafely(UserRateLimit item) {
                Map<String, CachedRateLimit> limitMap = item.getLimitMap()
                boolean containsKey = limitMap.containsKey(limitKey)
                boolean containsConfigLimitKey = limitMap.get(limitKey).configLimitKey ==
                        LimitKey.getConfigLimitKey(uriRegex, methods)
                boolean expiration = limitMap.get(limitKey).getNextExpirationTime() >= (startTime) &&
                        limitMap.get(limitKey).getNextExpirationTime() <= (startTime + timeUnit.toMillis(1))
                return containsKey &&
                       containsConfigLimitKey &&
                       expiration;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("An UserRateLimit with a limit key:" + limitKey + " methods: " + methods + " a time stamp near:" + startTime + " plus 1 " + timeUnit)
            }
        };
    }
}
