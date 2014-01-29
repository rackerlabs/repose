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
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = uriRegex
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.SECOND
        configuredRatelimit.value = 1
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(uriRegex)
        cachedRateLimit.usageMap.put(method, new LinkedList<Long>())
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap, false)
        long now = System.currentTimeMillis()
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, method, configuredRatelimit))
        assertThat(patchedLimit.withinLimit, equalTo(true))
        assertThat(patchedLimit, containsLimit(limitKey, method, now, TimeUnit.SECONDS, uriRegex))
    }

    @Test
    void "patch should not apply change when over limit"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = uriRegex
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.SECOND
        configuredRatelimit.value = 0
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(uriRegex)
        cachedRateLimit.usageMap.put(method, new LinkedList<Long>())
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap, false)
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, method, configuredRatelimit))
        assertThat(patchedLimit.withinLimit, equalTo(false))
        assertThat(patchedLimit.limitMap.get(limitKey).usageMap.get(method).isEmpty(), equalTo(true))
    }

    @Test
    void "patch should be returning a copy of the object"() {
        String limitKey = "testKey"
        HttpMethod method = HttpMethod.GET
        String uriRegex = "foo"
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = uriRegex
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.SECOND
        configuredRatelimit.value = 1
        HashMap<String, CachedRateLimit> startingLimitMap = new HashMap<String, CachedRateLimit>()
        CachedRateLimit cachedRateLimit = new CachedRateLimit(uriRegex)
        cachedRateLimit.usageMap.put(method, new LinkedList<Long>())
        startingLimitMap.put(limitKey, cachedRateLimit)

        UserRateLimit rateLimit = new UserRateLimit(startingLimitMap, false)
        UserRateLimit patchedLimit = rateLimit.applyPatch(new UserRateLimit.Patch(limitKey, method, configuredRatelimit))
        assertThat(patchedLimit, not(sameInstance(rateLimit)))
    }

    @Test
    void "newFromPatch should create a correct rate limit when within limit"() {
        long now = System.currentTimeMillis()

        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.unit = com.rackspace.repose.service.limits.schema.TimeUnit.MINUTE
        configuredRatelimit.value = 5
        UserRateLimit.Patch patch = new UserRateLimit.Patch("testKey", HttpMethod.GET, configuredRatelimit)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit.withinLimit, equalTo(true))
        assertThat(userRateLimit, containsLimit("testKey", HttpMethod.GET, now, TimeUnit.MINUTES, "foo"))
    }

    @Test
    void "newFromPatch should create a correct rate limit  when outside limit"() {
        ConfiguredRatelimit configuredRatelimit = new ConfiguredRatelimit()
        configuredRatelimit.uriRegex = "foo"
        configuredRatelimit.value = 0
        UserRateLimit.Patch patch = new UserRateLimit.Patch("testKey", HttpMethod.GET, configuredRatelimit)
        UserRateLimit userRateLimit = patch.newFromPatch()
        assertThat(userRateLimit.withinLimit, equalTo(false))
    }

    static Matcher<UserRateLimit> containsLimit(String limitKey, HttpMethod method, long startTime, TimeUnit timeUnit, String uriRegex) {
        return new TypeSafeMatcher<UserRateLimit>() {
            @Override
            protected boolean matchesSafely(UserRateLimit item) {
                HashMap<String,CachedRateLimit> limitMap = item.getLimitMap()
                boolean containsKey = limitMap.containsKey(limitKey)
                boolean containsUriRegex = limitMap.get(limitKey).regexHashcode == uriRegex.hashCode()
                boolean containsMethod = limitMap.get(limitKey).usageMap.containsKey(method)
                long expiration = limitMap.get(limitKey).usageMap.get(method).last
                boolean expectedExpiration = (expiration >= (TimeUnit.MILLISECONDS.convert(1, timeUnit) + startTime)) &&
                                             (expiration < (TimeUnit.MILLISECONDS.convert(2, timeUnit) + startTime))
                return containsKey &&
                       containsUriRegex &&
                       containsMethod &&
                       expectedExpiration;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("An UserRateLimit with a limit key:" + limitKey + " method: " + method + " a time stamp near:" + startTime + " plus 1 " + timeUnit)
            }
        };
    }
}
