package com.rackspace.repose.service.ratelimit.cache
import com.rackspace.repose.service.limits.schema.HttpMethod
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Test

import java.util.concurrent.TimeUnit

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.sameInstance
import static org.hamcrest.core.IsNot.not
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
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.SECOND, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(uriRegex)
        cachedRateLimit.usageMap.put(method, new Vector<Long>())
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)
        long now = System.currentTimeMillis()
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, configuredRatelimit))
        assertThat(patchedLimit, containsLimit(limitKey, [method], now, TimeUnit.SECONDS, uriRegex))
    }

    @Test
    void "patch should not apply change when over limit"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.SECOND, 0)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(uriRegex)
        cachedRateLimit.usageMap.put(method, new Vector<Long>())
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, configuredRatelimit))
        assertThat(patchedLimit.limitMap.get(limitKey).usageMap.isEmpty(), equalTo(true))
    }

    @Test
    void "patch should work when the limit key isn't yet in map"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.SECOND, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)
        long now = System.currentTimeMillis()
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, configuredRatelimit))
        assertThat(patchedLimit, containsLimit(limitKey, [method], now, TimeUnit.SECONDS, uriRegex))
    }

    @Test
    void "patch should be returning a different object than the one in the datastore"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = validConfiguredRateLimit(uriRegex, [method], com.rackspace.repose.service.limits.schema.TimeUnit.SECOND, 1)
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(uriRegex)
        cachedRateLimit.usageMap.put(method, new Vector<Long>())
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap)
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, configuredRatelimit))
        assertThat(patchedLimit, not(sameInstance(rateLimit)))
    }

    @Test
    void "newFromPatch should create a correct rate limit when within limit"() {
        long now = System.currentTimeMillis()

        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        configuredRatelimit.httpMethods = [HttpMethod.GET]
        UserRateLimit.Patch patch = new UserRateLimit.Patch("testKey", configuredRatelimit)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit, containsLimit("testKey", [HttpMethod.GET], now, TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit with multiple http methods when within limit"() {
        long now = System.currentTimeMillis()

        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        configuredRatelimit.httpMethods = [HttpMethod.GET, HttpMethod.POST]
        UserRateLimit.Patch patch = new UserRateLimit.Patch("testKey", configuredRatelimit)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit, containsLimit("testKey", [HttpMethod.GET, HttpMethod.POST], now, TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit  when outside limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.value = 0
        configuredRatelimit.httpMethods = [HttpMethod.GET]
        UserRateLimit.Patch patch = new UserRateLimit.Patch("testKey", configuredRatelimit)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit.limitMap.get("testKey").usageMap.size(), equalTo(0))
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
                HashMap<String,CachedRateLimit> limitMap = item.getLimitMap()
                boolean containsKey = limitMap.containsKey(limitKey)
                boolean containsUriRegex = limitMap.get(limitKey).regexHashcode == uriRegex.hashCode()
                boolean containsMethods = limitMap.get(limitKey).usageMap.keySet().containsAll(methods)
                boolean expiration = limitMap.get(limitKey).usageMap.entrySet.every {
                    (it.value.last >= (TimeUnit.MILLISECONDS.convert(1, timeUnit) + startTime)) &&
                    (it.value.last < (TimeUnit.MILLISECONDS.convert(2, timeUnit) + startTime))
                }
                return containsKey &&
                       containsUriRegex &&
                       containsMethods &&
                       expiration;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("An UserRateLimit with a limit key:" + limitKey + " methods: " + methods + " a time stamp near:" + startTime + " plus 1 " + timeUnit)
            }
        };
    }
}
