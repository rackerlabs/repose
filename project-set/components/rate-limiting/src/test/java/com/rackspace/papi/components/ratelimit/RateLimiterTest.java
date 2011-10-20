package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.ratelimit.cache.NextAvailableResponse;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import org.junit.Test;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RateLimiterTest extends RateLimitingTestSupport {

    public static class WhenCreatingNewInstances {

        private RateLimiter rateLimiter;

        @Before
        public void standUp() throws Exception {
            final RateLimitCache cache = mock(RateLimitCache.class);
            when(cache.updateLimit(any(HttpMethod.class), anyString(), anyString(), any(ConfiguredRatelimit.class))).thenReturn(new NextAvailableResponse(true, Calendar.getInstance().getTime()));
            
            final RateLimitingConfiguration cfg = defaultRateLimitingConfiguration();
            
            rateLimiter = new RateLimiter(cache, cfg, newRegexCache(cfg.getLimitGroup()));
        }

        @Test
        public void shouldPassWhenRequestRemain() {
            final RateLimitingRequestInfo requestInfo = mock(RateLimitingRequestInfo.class);
            final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
            
            when(requestInfo.getUserName()).thenReturn("user");
            when(requestInfo.getFirstUserGroup()).thenReturn("group");
            when(requestInfo.getRequestMethod()).thenReturn(HttpMethod.GET);
            when(requestInfo.getRequest()).thenReturn(servletRequest);
            when(servletRequest.getRequestURI()).thenReturn("/v1.0/12345/resource");
            
            final FilterDirector director = new FilterDirectorImpl();
            rateLimiter.recordLimitedRequest(requestInfo, director);
            
            assertEquals(FilterAction.NOT_SET, director.getFilterAction());
        }
    }
}
