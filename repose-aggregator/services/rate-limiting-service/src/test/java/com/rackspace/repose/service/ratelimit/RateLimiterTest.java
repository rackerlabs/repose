package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.cache.NextAvailableResponse;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.repose.service.ratelimit.exception.CacheException;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RateLimiterTest {

   public static class WhenHandlingRateLimits {

      private static final String USER = "a user";
      private static final String URI = "/some/uri/";
      private final RateLimitCache mockedCache = mock(RateLimitCache.class);
      private final LimitKey limitKey = new LimitKey();
      private final Pattern pattern = Pattern.compile("/some/uri/(.*)");
      private final Matcher uriMatcher = pattern.matcher(URI);
      private final ConfiguredRatelimit configuredRateLimit = RateLimitingTestSupport.defaultRateLimitingConfiguration().getLimitGroup().get(0).getLimit().get(0);
      private String key;
      private int datastoreWarnLimit= 1000;

      public WhenHandlingRateLimits() {
         uriMatcher.matches();
         key = limitKey.getLimitKey(URI, uriMatcher, true);
      }
      
      @Test(expected=OverLimitException.class)
      public void noCaptureGroupshouldThrowOverLimitException() throws IOException, OverLimitException {

         final RateLimiter rateLimiter = new RateLimiter(mockedCache);

         when(mockedCache.updateLimit(any(HttpMethod.class), any(String.class), any(String.class),
                                      any(ConfiguredRatelimit.class), anyInt())).thenReturn(new NextAvailableResponse(false, new Date(), 10));

         key = limitKey.getLimitKey(URI, uriMatcher, false);
         rateLimiter.handleRateLimit(USER, key, configuredRateLimit, datastoreWarnLimit);
      }


      @Test(expected=OverLimitException.class)
      public void shouldThrowOverLimitException() throws IOException, OverLimitException {

         final RateLimiter rateLimiter = new RateLimiter(mockedCache);

         when(mockedCache.updateLimit(any(HttpMethod.class), any(String.class), any(String.class),
                                      any(ConfiguredRatelimit.class), anyInt())).thenReturn(new NextAvailableResponse(false, new Date(), 10));


         rateLimiter.handleRateLimit(USER, key, configuredRateLimit, datastoreWarnLimit);
      }

      @Test(expected= CacheException.class)
      public void shouldThrowCacheException() throws OverLimitException, IOException {
         final RateLimiter rateLimiter = new RateLimiter(mockedCache);

         when(mockedCache.updateLimit(any(HttpMethod.class), any(String.class), any(String.class),
                                      any(ConfiguredRatelimit.class), anyInt())).thenThrow(new IOException("uh oh"));

         rateLimiter.handleRateLimit(USER, key, configuredRateLimit, datastoreWarnLimit);

      }

      @Test
      public void shouldUpdateLimitWithoutExceptions() throws IOException, OverLimitException {
         final RateLimiter rateLimiter = new RateLimiter(mockedCache);

         when(mockedCache.updateLimit(any(HttpMethod.class), any(String.class), any(String.class), any(ConfiguredRatelimit.class), anyInt())).thenReturn(new NextAvailableResponse(true, new Date(), 10));

         rateLimiter.handleRateLimit(USER, key, configuredRateLimit, datastoreWarnLimit);
      }
   }   
}
