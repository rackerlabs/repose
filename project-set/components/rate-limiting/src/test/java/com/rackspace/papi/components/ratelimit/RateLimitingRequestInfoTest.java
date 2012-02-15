package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RateLimitingRequestInfoTest {

   private static final String MOST_QUALIFIED_USER = "the best user of them all";
   private static final MediaType MEDIA_TYPE = new MediaType(MimeType.APPLICATION_XML);

   public static class WhenInstantiatingRequestInformation {

      protected HttpServletRequest mockedRequest;

      @Before
      public void standUp() {
         mockedRequest = mock(HttpServletRequest.class);

         when(mockedRequest.getMethod()).thenReturn("GET");

         List<String> headerValues = new LinkedList<String>();
         headerValues.add("group-4;q=0.1");
         headerValues.add("group-2;q=0.1");
         headerValues.add("group-1;q=0.1");
         headerValues.add("group-3;q=0.1");
         headerValues.add("group-3;q=0.002");

         when(mockedRequest.getHeaders(PowerApiHeader.GROUPS.toString())).thenReturn(Collections.enumeration(headerValues));

         headerValues = new LinkedList<String>();
         headerValues.add(MOST_QUALIFIED_USER + ";q=1.0");
         headerValues.add("that other user;q=0.5");
         headerValues.add("127.0.0.1;q=0.1");

         when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenReturn(Collections.enumeration(headerValues));
      }

      @Test
      public void shouldCopyGroupHeaders() {
         final RateLimitingRequestInfo info = new RateLimitingRequestInfo(mockedRequest,MEDIA_TYPE);

         assertEquals("Request info must copy user group information from request", 4, info.getUserGroups().size());
      }

      @Test
      public void shouldSelectMostQualifiedUserHeader() {
         final RateLimitingRequestInfo info = new RateLimitingRequestInfo(mockedRequest,MEDIA_TYPE);

         assertEquals("Request info must understand user header quality", MOST_QUALIFIED_USER, info.getUserName());
      }
   }

   public static class WhenGettingRateLimitingRequestInformation {

      protected HttpServletRequest mockedRequest;

      @Before
      public void standUp() {
         mockedRequest = mock(HttpServletRequest.class);

         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getHeaders(PowerApiHeader.GROUPS.toString())).thenReturn(Collections.enumeration(Collections.EMPTY_LIST));
         when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenReturn(Collections.enumeration(Collections.EMPTY_LIST));
      }

      @Test
      public void shouldReturnUserWithoutQualityFactorsPresent() {
         final List<String> headerValues = new LinkedList<String>();
         headerValues.add(MOST_QUALIFIED_USER);
         
         when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenReturn(Collections.enumeration(headerValues));
         
         final RateLimitingRequestInfo info = new RateLimitingRequestInfo(mockedRequest,MEDIA_TYPE);

         assertEquals("Rate limiting request info must return correct user without quality factors", MOST_QUALIFIED_USER, info.getUserName());
      }

      @Test
      public void shouldReturnNullWhenNoGroupsHeaderIsPresent() {
         final RateLimitingRequestInfo info = new RateLimitingRequestInfo(mockedRequest,MEDIA_TYPE);

         assertTrue("Rate limiting request info must return null for groups when no group information is present in the request", info.getFirstUserGroup() == null);
      }

      @Test
      public void shouldReturnFirstGroup() {
         final RateLimitingRequestInfo info = new RateLimitingRequestInfo(mockedRequest,MEDIA_TYPE);

         assertTrue("Rate limiting request info must return null for groups when no group information is present in the request", info.getFirstUserGroup() == null);
      }
   }
}
