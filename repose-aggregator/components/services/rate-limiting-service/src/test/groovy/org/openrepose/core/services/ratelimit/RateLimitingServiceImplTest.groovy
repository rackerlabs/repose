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
package org.openrepose.core.services.ratelimit

import org.apache.commons.lang3.tuple.Pair
import org.junit.Before
import org.junit.Test
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit
import org.openrepose.core.services.ratelimit.cache.ManagedRateLimitCache
import org.openrepose.core.services.ratelimit.cache.NextAvailableResponse
import org.openrepose.core.services.ratelimit.cache.RateLimitCache
import org.openrepose.core.services.ratelimit.config.*
import org.openrepose.core.services.ratelimit.exception.OverLimitException

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.*
import static org.mockito.Matchers.*
import static org.mockito.Mockito.*

class RateLimitingServiceImplTest extends RateLimitServiceTestContext {
    private Map<String, CachedRateLimit> cacheMap
    private ConfiguredLimitGroup configuredLimitGroup
    private ConfiguredLimitGroup queryParamLimitGroup
    private ConfiguredLimitGroup defaultMethodsLimitGroup
    private int datastoreWarnLimit = 1000

    private ConfiguredRatelimit mockConfiguredRateLimit
    private CachedRateLimit mockCachedRateLimit

    private RateLimitingService rateLimitingService
    private RateLimitCache cache
    private RateLimitingConfiguration config

    @Before
    final void standUp() {
        mockConfiguredRateLimit = mock(ConfiguredRatelimit.class)
        mockCachedRateLimit = mock(CachedRateLimit.class)

        cache = mock(ManagedRateLimitCache.class)
        config = new RateLimitingConfiguration()
        config.setUseCaptureGroups(true)

        cacheMap = new HashMap<String, CachedRateLimit>()
        configuredLimitGroup = new ConfiguredLimitGroup()
        queryParamLimitGroup = new ConfiguredLimitGroup()
        defaultMethodsLimitGroup = new ConfiguredLimitGroup()

        configuredLimitGroup.setDefault(true)
        configuredLimitGroup.setId("configured-limit-group")
        configuredLimitGroup.getGroups().add("user")

        queryParamLimitGroup.setId("query-param-group")
        queryParamLimitGroup.getGroups().add("query-param-user")

        defaultMethodsLimitGroup.setId("default-methods-group")
        defaultMethodsLimitGroup.getGroups().add("methods-user")

        LinkedList<HttpMethod> methods = new LinkedList<HttpMethod>(), getMethod = new LinkedList<HttpMethod>()
        methods.add(HttpMethod.GET)
        methods.add(HttpMethod.PUT)
        methods.add(HttpMethod.POST)
        methods.add(HttpMethod.DELETE)
        getMethod.add(HttpMethod.GET)

        LinkedList<String> queryNames = new LinkedList<String>()

        cacheMap.put(SIMPLE_URI, new CachedRateLimit(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods, queryNames)))

        configuredLimitGroup.getLimit().add(newLimitConfig(SIMPLE_ID, SIMPLE_URI, SIMPLE_URI_REGEX, methods, queryNames))

        cacheMap.put(COMPLEX_URI_REGEX, new CachedRateLimit(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods, queryNames)))

        configuredLimitGroup.getLimit().add(newLimitConfig(COMPLEX_ID, COMPLEX_URI, COMPLEX_URI_REGEX, methods, queryNames))

        configuredLimitGroup.getLimit().add(newLimitConfig("groups-id", GROUPS_URI, GROUPS_URI_REGEX, getMethod, queryNames))

        queryParamLimitGroup.getLimit().add(newLimitConfig("query-param-test", "*", ".*", methods, ["index"].asList()))

        defaultMethodsLimitGroup.getLimit().add(newLimitConfig("methods-test-empty", "/methods/empty/*", "/methods/empty/.*", Collections.emptyList(), queryNames))
        defaultMethodsLimitGroup.getLimit().add(newLimitConfig("methods-test-all", "/methods/all/*", "/methods/all/.*", Collections.singletonList(HttpMethod.ALL), queryNames))

        config.getLimitGroup().add(queryParamLimitGroup)
        config.getLimitGroup().add(configuredLimitGroup)
        config.getLimitGroup().add(defaultMethodsLimitGroup)

        ConfiguredRatelimit globalLimit = new ConfiguredRatelimit()
        globalLimit.setId("catch-all")
        globalLimit.setUnit(TimeUnit.MINUTE)
        globalLimit.setUri(".*")
        globalLimit.setUriRegex(".*")
        globalLimit.setValue(1)
        globalLimit.getHttpMethods().add(HttpMethod.ALL)
        ConfiguredRateLimitWrapper globalLimitWrapper = new ConfiguredRateLimitWrapper(globalLimit)
        GlobalLimitGroup globalLimitGroup = new GlobalLimitGroup()
        globalLimitGroup.getLimit().add(globalLimitWrapper)
        config.setGlobalLimitGroup(globalLimitGroup)

        when(cache.getUserRateLimits("usertest1")).thenReturn(cacheMap)

        rateLimitingService = new RateLimitingServiceImpl(cache, config)
    }

    @Test(expected = IllegalArgumentException.class)
    void shouldReturnExceptionOnNullConfiguration() {
        new RateLimitingServiceImpl(cache, null)
    }

    @Test
    void shouldReturnLimitsOnQuery() {

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")
        RateLimitList list = rateLimitingService.queryLimits("user", groups)

        assertNotNull(list)
    }

    @Test(expected = IllegalArgumentException.class)
    void shouldReturnExceptionOnNullUser() {
        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        rateLimitingService.queryLimits(null, groups)
    }

    @Test
    void shouldTrackLimits() throws IOException, OverLimitException {
        when(mockCachedRateLimit.amount()).thenReturn(1)
        when(mockCachedRateLimit.maxAmount()).thenReturn(2)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(new Date().getTime())

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt()))
            .thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        rateLimitingService.trackLimits("user", groups, "/loadbalancer/something", null, "GET", datastoreWarnLimit)
    }

    @Test
    void shouldThrowOverLimits() throws IOException, OverLimitException {
        Date nextAvail = new Date()

        when(mockCachedRateLimit.amount()).thenReturn(0)
        when(mockCachedRateLimit.maxAmount()).thenReturn(0)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(nextAvail.getTime())
        when(mockConfiguredRateLimit.toString()).thenReturn("value=20")

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt()))
            .thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        try {
            rateLimitingService.trackLimits("user", groups, "/loadbalancer/something", null, "GET", datastoreWarnLimit)
        } catch (OverLimitException e) {
            assertEquals("User should be returned", e.getUser(), "user")
            assertThat("Next available time should be returned", e.getNextAvailableTime() <=> nextAvail, equalTo(0))
            assertThat("Configured limits should be returned", e.getConfiguredLimit(), containsString("value=20"))
            assertEquals(0, e.getCurrentLimitAmount())
        }
    }

    @Test
    void shouldThrowOverLimitsWithMethodsEmpty() throws IOException, OverLimitException {
        Date nextAvail = new Date()

        when(mockCachedRateLimit.amount()).thenReturn(0)
        when(mockCachedRateLimit.maxAmount()).thenReturn(0)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(nextAvail.getTime())
        when(mockConfiguredRateLimit.toString()).thenReturn("value=20")

        when(cache.updateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt()))
            .thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        try {
            rateLimitingService.trackLimits(
                "user",
                Collections.singletonList("methods-user"),
                "/methods/empty/123",
                null,
                "GET",
                datastoreWarnLimit)
        } catch (OverLimitException e) {
            assertEquals("User should be returned", e.getUser(), "user")
            assertThat("Next available time should be returned", e.getNextAvailableTime() <=> nextAvail, equalTo(0))
            assertThat("Configured limits should be returned", e.getConfiguredLimit(), containsString("value=20"))
            assertEquals(0, e.getCurrentLimitAmount())
        }
    }

    @Test
    void shouldThrowOverLimitsWithMethodsAll() throws IOException, OverLimitException {
        Date nextAvail = new Date()

        when(mockCachedRateLimit.amount()).thenReturn(0)
        when(mockCachedRateLimit.maxAmount()).thenReturn(0)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(nextAvail.getTime())
        when(mockConfiguredRateLimit.toString()).thenReturn("value=20")

        when(cache.updateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt()))
            .thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        try {
            rateLimitingService.trackLimits(
                "user",
                Collections.singletonList("methods-user"),
                "/methods/all/456",
                null,
                "GET",
                datastoreWarnLimit)
        } catch (OverLimitException e) {
            assertEquals("User should be returned", e.getUser(), "user")
            assertThat("Next available time should be returned", e.getNextAvailableTime() <=> nextAvail, equalTo(0))
            assertThat("Configured limits should be returned", e.getConfiguredLimit(), containsString("value=20"))
            assertEquals(0, e.getCurrentLimitAmount())
        }
    }

    @Test(expected = IllegalArgumentException.class)
    void shouldThrowIllegalArgumentsOnNullUser() throws IOException, OverLimitException {
        when(mockCachedRateLimit.amount()).thenReturn(1)
        when(mockCachedRateLimit.maxAmount()).thenReturn(2)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(new Date().getTime())

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt()))
            .thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        rateLimitingService.trackLimits(null, groups, "/loadbalancer/something", null, "GET", datastoreWarnLimit)
    }


    @Test
    void shouldTrackNOGROUPSLimits() throws IOException, OverLimitException {
        when(mockCachedRateLimit.amount()).thenReturn(1)
        when(mockCachedRateLimit.maxAmount()).thenReturn(2)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(new Date().getTime())

        List<String> groups = new ArrayList<String>()
        config.setUseCaptureGroups(Boolean.FALSE)
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt()))
            .thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        rateLimitingService.trackLimits("user", groups, "/loadbalancer/something/1234", null, "GET", datastoreWarnLimit)
    }

    @Test
    void shouldTrackQueryParamLimits() {
        RateLimiter limiter = mock(RateLimiter.class)

        RateLimitingServiceImpl rateLimitingService = new RateLimitingServiceImpl(null, config)
        rateLimitingService.rateLimiter = limiter

        rateLimitingService.trackLimits("testUser", ["query-param-user"].asList(), "/query/test", ["index": ["0"] as String[]], "GET", 1000)

        verify(limiter, times(2)).handleRateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt())
    }

    @Test
    void shouldTrackGlobalLimits() {
        RateLimiter limiter = mock(RateLimiter.class)

        RateLimitingServiceImpl rateLimitingService = new RateLimitingServiceImpl(null, config)
        rateLimitingService.rateLimiter = limiter

        rateLimitingService.trackLimits("testUser", new ArrayList<String>(), "/global/test", new HashMap<String, String[]>(), "GET", 1000)

        verify(limiter).handleRateLimit(eq("GlobalLimitUser"), any(List.class), eq(TimeUnit.MINUTE), eq(1000))
    }

    @Test(expected = OverLimitException.class)
    void shouldDefaultToAllHttpMethodsForGlobalLimits() {
        RateLimiter limiter = mock(RateLimiter.class)
        doThrow(new OverLimitException("User rate limited!", "GlobalLimitUser", new Date(), 1, "1"))
            .when(limiter).handleRateLimit(eq("GlobalLimitUser"), any(List.class), eq(TimeUnit.MINUTE), eq(1000))

        ConfiguredRatelimit globalLimit = config.getGlobalLimitGroup().getLimit().get(0)
        globalLimit.httpMethods.clear()

        RateLimitingServiceImpl rateLimitingService = new RateLimitingServiceImpl(null, config)
        rateLimitingService.rateLimiter = limiter

        rateLimitingService.trackLimits("testUser", new ArrayList<String>(), "/global/test", new HashMap<String, String[]>(), "GET", 1000)
    }
}
