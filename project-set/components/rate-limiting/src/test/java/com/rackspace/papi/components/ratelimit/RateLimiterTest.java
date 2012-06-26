package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.ratelimit.cache.NextAvailableResponse;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.rackspace.papi.components.ratelimit.FilterDirectorTestHelper.directorMustAddHeaderToRequest;
import static com.rackspace.papi.components.ratelimit.FilterDirectorTestHelper.directorMustAddHeaderToResponse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RateLimiterTest extends RateLimitingTestSupport {

   public static class WhenCreatingNewInstances {

      private RateLimitingConfiguration cfg;
      private RateLimitCache cacheMock;
      private RateLimiter rateLimiter;

      @Before
      public void standUp() throws Exception {
         cacheMock = mock(RateLimitCache.class);

         cfg = defaultRateLimitingConfiguration();
      }

      private void mockLimits() throws IOException {
         when(cacheMock.updateLimit(eq(HttpMethod.GET), anyString(), anyString(), any(ConfiguredRatelimit.class))).thenReturn(new NextAvailableResponse(true, Calendar.getInstance().getTime(), 1));
         when(cacheMock.updateLimit(eq(HttpMethod.PUT), anyString(), anyString(), any(ConfiguredRatelimit.class))).thenReturn(new NextAvailableResponse(false, Calendar.getInstance().getTime(), 1));
         when(cacheMock.updateLimit(eq(HttpMethod.POST), anyString(), anyString(), any(ConfiguredRatelimit.class))).thenReturn(new NextAvailableResponse(false, Calendar.getInstance().getTime(), 1));

         rateLimiter = new RateLimiter(cacheMock, newRegexCache(cfg.getLimitGroup()), cfg);
      }

      @Test
      public void shouldPassWhenRequestRemain() throws Exception {
         mockLimits();

         final RateLimitingRequestInfo requestInfo = mock(RateLimitingRequestInfo.class);
         final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
         final List headers = new ArrayList<HeaderValue>();
         headers.add(new HeaderValueImpl("group"));

         when(requestInfo.getUserName()).thenReturn(new HeaderValueImpl("user"));
         when(requestInfo.getUserGroups()).thenReturn(headers);
         when(requestInfo.getRequestMethod()).thenReturn(HttpMethod.GET);
         when(requestInfo.getRequest()).thenReturn(servletRequest);
         when(servletRequest.getRequestURI()).thenReturn("/v1.0/12345/resource");

         final FilterDirector director = new FilterDirectorImpl();
         rateLimiter.recordLimitedRequest(requestInfo, director);

         assertEquals(FilterAction.NOT_SET, director.getFilterAction());
         
         verify(cacheMock, times(1)).updateLimit(eq(HttpMethod.GET), anyString(), anyString(), any(ConfiguredRatelimit.class));
      }

      @Test
      public void shouldRejectLimitedRequestWithDelegationDisabled() throws Exception {
         mockLimits();

         final RateLimitingRequestInfo requestInfo = mock(RateLimitingRequestInfo.class);
         final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
         final List headers = new ArrayList<HeaderValue>();
         headers.add(new HeaderValueImpl("group"));

         when(requestInfo.getUserName()).thenReturn(new HeaderValueImpl("user"));
         when(requestInfo.getUserGroups()).thenReturn(headers);
         when(requestInfo.getRequestMethod()).thenReturn(HttpMethod.PUT);
         when(requestInfo.getRequest()).thenReturn(servletRequest);
         when(servletRequest.getRequestURI()).thenReturn("/v1.0/12345/resource");

         final FilterDirector director = new FilterDirectorImpl();
         rateLimiter.recordLimitedRequest(requestInfo, director);

         assertEquals(FilterAction.RETURN, director.getFilterAction());
         assertEquals(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE, director.getResponseStatus());
         
         directorMustAddHeaderToResponse(director, CommonHttpHeader.RETRY_AFTER.toString());
         
         verify(cacheMock, times(1)).updateLimit(eq(HttpMethod.PUT), anyString(), anyString(), any(ConfiguredRatelimit.class));
      }

      @Test
      public void shouldPassLimitedRequestWithDelegationEnabled() throws Exception {
         mockLimits();

         cfg.setDelegation(Boolean.TRUE);
         rateLimiter = new RateLimiter(cacheMock, newRegexCache(cfg.getLimitGroup()), cfg);
         
         final RateLimitingRequestInfo requestInfo = mock(RateLimitingRequestInfo.class);
         final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
         final List headers = new ArrayList<HeaderValue>();
         headers.add(new HeaderValueImpl("group"));

         when(requestInfo.getUserName()).thenReturn(new HeaderValueImpl("user"));
         when(requestInfo.getUserGroups()).thenReturn(headers);
         when(requestInfo.getRequestMethod()).thenReturn(HttpMethod.POST);
         when(requestInfo.getRequest()).thenReturn(servletRequest);
         when(servletRequest.getRequestURI()).thenReturn("/v1.0/12345/resource");

         final FilterDirector director = new FilterDirectorImpl();
         rateLimiter.recordLimitedRequest(requestInfo, director);

         assertEquals(FilterAction.PASS, director.getFilterAction());
         
         directorMustAddHeaderToRequest(director, PowerApiHeader.RATE_LIMITED.toString());
         
         verify(cacheMock, times(1)).updateLimit(eq(HttpMethod.POST), anyString(), anyString(), any(ConfiguredRatelimit.class));
      }
   }
}
