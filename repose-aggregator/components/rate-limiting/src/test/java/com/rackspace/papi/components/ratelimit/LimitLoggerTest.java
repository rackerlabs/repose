package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.components.ratelimit.log.LimitLogger;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class LimitLoggerTest {

   public static class WhenSanitizing {
      final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);

      @Test
      public void shouldReturnUsername() {
         final LimitLogger logger = new LimitLogger("some_username", mockedRequest);
         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(null);

         final String userId = logger.getSanitizedUserIdentification();
         
         assertEquals(userId, "some_username");
      }

      @Test
      public void shouldReturnXForwardedFor() {
         final LimitLogger logger = new LimitLogger("some_username", mockedRequest);

         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("some_username");
         when(mockedRequest.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn("x-forwarded-for-value");

         final String userId = logger.getSanitizedUserIdentification();

         assertEquals(userId, "x-forwarded-for-value");
      }

      @Test
      public void shouldReturnRequestRemoteHost() {
         final LimitLogger logger = new LimitLogger("some_username", mockedRequest);

         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("some_username");
         when(mockedRequest.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn(null);
         when(mockedRequest.getRemoteHost()).thenReturn("remote-host-value");

         final String userId = logger.getSanitizedUserIdentification();

         assertEquals(userId, "remote-host-value");
      }
   }
}
