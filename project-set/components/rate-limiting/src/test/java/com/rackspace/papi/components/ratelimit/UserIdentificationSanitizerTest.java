package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class UserIdentificationSanitizerTest {

   public static class WhenSanitizing {
      final MutableHttpServletRequest mockedRequest = mock(MutableHttpServletRequest.class);
      final RateLimitingRequestInfo mockedRequestInfo = mock(RateLimitingRequestInfo.class);

      @Test
      public void shouldReturnUsername() {
         when(mockedRequestInfo.getUserName()).thenReturn(new MyHeaderValue());
         when(mockedRequestInfo.getRequest()).thenReturn(mockedRequest);

         final UserIdentificationSanitizer sanitizer = new UserIdentificationSanitizer(mockedRequestInfo);
         final String userId = sanitizer.getUserIdentification();

         assertEquals(userId, "some_username");
      }

      @Test
      public void shouldReturnXForwardedFor() {
         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("some_username");
         when(mockedRequest.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn("x-forwarded-for-value");

         when(mockedRequestInfo.getUserName()).thenReturn(new MyHeaderValue());
         when(mockedRequestInfo.getRequest()).thenReturn(mockedRequest);

         final UserIdentificationSanitizer sanitizer = new UserIdentificationSanitizer(mockedRequestInfo);
         final String userId = sanitizer.getUserIdentification();

         assertEquals(userId, "x-forwarded-for-value");
      }

      @Test
      public void shouldReturnRequestRemoteHost() {
         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("some_username");
         when(mockedRequest.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn(null);
         when(mockedRequest.getRemoteHost()).thenReturn("remote-host-value");

         when(mockedRequestInfo.getUserName()).thenReturn(new MyHeaderValue());
         when(mockedRequestInfo.getRequest()).thenReturn(mockedRequest);

         final UserIdentificationSanitizer sanitizer = new UserIdentificationSanitizer(mockedRequestInfo);
         final String userId = sanitizer.getUserIdentification();

         assertEquals(userId, "remote-host-value");
      }

      public static class MyHeaderValue implements HeaderValue {

         @Override
         public String getValue() {
            return "some_username";
         }

         @Override
         public Map<String, String> getParameters() {
            return null;
         }

         @Override
         public double getQualityFactor() {
            return 0;
         }

         @Override
         public int compareTo(HeaderValue headerValue) {
            return 0;
         }
      }
   }
}
