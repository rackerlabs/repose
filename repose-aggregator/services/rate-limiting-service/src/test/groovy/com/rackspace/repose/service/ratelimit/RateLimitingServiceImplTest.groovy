package com.rackspace.repose.service.ratelimit

import com.rackspace.repose.service.limits.schema.HttpMethod
import com.rackspace.repose.service.limits.schema.RateLimitList
import com.rackspace.repose.service.limits.schema.TimeUnit
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit
import com.rackspace.repose.service.ratelimit.cache.ManagedRateLimitCache
import com.rackspace.repose.service.ratelimit.cache.NextAvailableResponse
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache
import com.rackspace.repose.service.ratelimit.config.*
import com.rackspace.repose.service.ratelimit.exception.OverLimitException
import org.apache.commons.lang3.tuple.Pair
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*
import static org.mockito.Matchers.eq
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyInt
import static org.mockito.Mockito.*

public class RateLimitingServiceImplTest extends RateLimitServiceTestContext {
    private Map<String, CachedRateLimit> cacheMap
    private ConfiguredLimitGroup configuredLimitGroup
    private ConfiguredLimitGroup queryParamLimitGroup
    private int datastoreWarnLimit = 1000

    private ConfiguredRatelimit mockConfiguredRateLimit
    private CachedRateLimit mockCachedRateLimit

    private RateLimitingService rateLimitingService
    private RateLimitCache cache
    private RateLimitingConfiguration config

    @Before
    public final void standUp() {
        mockConfiguredRateLimit = mock(ConfiguredRatelimit.class)
        mockCachedRateLimit = mock(CachedRateLimit.class)

        cache = mock(ManagedRateLimitCache.class)
        config = new RateLimitingConfiguration()
        config.setUseCaptureGroups(true)

        cacheMap = new HashMap<String, CachedRateLimit>()
        configuredLimitGroup = new ConfiguredLimitGroup()
        queryParamLimitGroup = new ConfiguredLimitGroup()

        configuredLimitGroup.setDefault(true)
        configuredLimitGroup.setId("configured-limit-group")
        configuredLimitGroup.getGroups().add("user")

        queryParamLimitGroup.setId("query-param-group")
        queryParamLimitGroup.getGroups().add("query-param-user")

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

        config.getLimitGroup().add(queryParamLimitGroup)
        config.getLimitGroup().add(configuredLimitGroup)

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
    public void shouldReturnExceptionOnNullConfiguration() {
        RateLimitingService invalidService = null

        invalidService = new RateLimitingServiceImpl(cache, null)
    }

    @Test
    public void shouldReturnLimitsOnQuery() {

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")
        RateLimitList list = rateLimitingService.queryLimits("user", groups)

        assertNotNull(list)
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReturnExceptionOnNullUser() {
        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")
        RateLimitList list = null

        list = rateLimitingService.queryLimits(null, groups)
    }

    @Test
    public void shouldTrackLimits() throws IOException, OverLimitException {
        when(mockCachedRateLimit.amount()).thenReturn(1)
        when(mockCachedRateLimit.maxAmount()).thenReturn(2)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(new Date().getTime())

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        rateLimitingService.trackLimits("user", groups, "/loadbalancer/something", null, "GET", datastoreWarnLimit)
    }

    @Test
    public void shouldThrowOverLimits() throws IOException, OverLimitException {
        Date nextAvail = new Date()

        when(mockCachedRateLimit.amount()).thenReturn(0)
        when(mockCachedRateLimit.maxAmount()).thenReturn(0)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(nextAvail.getTime())
        when(mockConfiguredRateLimit.toString()).thenReturn("value=20")

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        try {
            rateLimitingService.trackLimits("user", groups, "/loadbalancer/something", null, "GET", datastoreWarnLimit)
        } catch (OverLimitException e) {
            assertEquals("User should be returned", e.getUser(), "user")
            assertTrue("Next available time should be returned", e.getNextAvailableTime().compareTo(nextAvail) == 0)
            assertTrue("Configured limits should be returned", e.getConfiguredLimit().contains("value=20"))
            assertEquals(0, e.getCurrentLimitAmount())
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentsOnNullUser() throws IOException, OverLimitException {
        when(mockCachedRateLimit.amount()).thenReturn(1)
        when(mockCachedRateLimit.maxAmount()).thenReturn(2)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(new Date().getTime())

        List<String> groups = new ArrayList<String>()
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        rateLimitingService.trackLimits(null, groups, "/loadbalancer/something", null, "GET", datastoreWarnLimit)
    }


    @Test
    public void shouldTrackNOGROUPSLimits() throws IOException, OverLimitException {
        when(mockCachedRateLimit.amount()).thenReturn(1)
        when(mockCachedRateLimit.maxAmount()).thenReturn(2)
        when(mockCachedRateLimit.getNextExpirationTime()).thenReturn(new Date().getTime())

        List<String> groups = new ArrayList<String>()
        config.setUseCaptureGroups(Boolean.FALSE)
        groups.add("configure-limit-group")

        when(cache.updateLimit(any(String.class), any(List.class),
                any(TimeUnit.class), anyInt())).thenReturn(new NextAvailableResponse(Pair.of(mockConfiguredRateLimit, mockCachedRateLimit)))

        rateLimitingService.trackLimits("user", groups, "/loadbalancer/something/1234", null, "GET", datastoreWarnLimit)
    }

    @Test
    public void shouldTrackQueryParamLimits() {
        RateLimiter limiter = mock(RateLimiter.class)

        RateLimitingServiceImpl rateLimitingService = new RateLimitingServiceImpl(null, config)
        rateLimitingService.rateLimiter = limiter

        rateLimitingService.trackLimits("testUser", ["query-param-user"].asList(), "/query/test", ["index" : ["0"] as String[]], "GET", 1000)

        verify(limiter, times(2)).handleRateLimit(any(String.class), any(List.class), any(TimeUnit.class), anyInt())
    }

    @Test
    public void shouldTrackGlobalLimits() {
        RateLimiter limiter = mock(RateLimiter.class)

        RateLimitingServiceImpl rateLimitingService = new RateLimitingServiceImpl(null, config)
        rateLimitingService.rateLimiter = limiter

        rateLimitingService.trackLimits("testUser", new ArrayList<String>(), "/global/test", new HashMap<String, String[]>(), "GET", 1000)

        verify(limiter).handleRateLimit(eq("GlobalLimitUser"), any(List.class), eq(TimeUnit.MINUTE), eq(1000))
    }
}
