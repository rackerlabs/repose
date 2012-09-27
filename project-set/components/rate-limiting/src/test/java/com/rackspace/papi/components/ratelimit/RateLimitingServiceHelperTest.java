package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.media.MimeType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MediaType;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RateLimitingServiceHelperTest {

   public static class WhenGettingPreferredMediaType {
      private final RateLimitingServiceHelper helper = new RateLimitingServiceHelper(null, null, null);

      @Test
      public void shouldGetJavaMediaTypeFromReposeMimeType() {
         MimeType reposeMimeType = MimeType.APPLICATION_XML;

         MediaType javaMediaType = helper.getJavaMediaType(reposeMimeType);

         assertEquals(MediaType.APPLICATION_XML, javaMediaType.toString());
      }
   }

   public static class WhenGettingPreferredUser {

      private static final String MOST_QUALIFIED_USER = "the best user of them all";
      private final RateLimitingServiceHelper helper = new RateLimitingServiceHelper(null, null, null);
      protected HttpServletRequest mockedRequest;

      @Before
      public void standUp() {
         mockedRequest = mock(HttpServletRequest.class);
         final List<String> headerNames = new LinkedList<String>();
         headerNames.add(PowerApiHeader.USER.toString());

         when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));
      }

      @Test
      public void shouldReturnMostQualifiedUserHeader() {
         List<String> headerValues = new LinkedList<String>();
         headerValues.add(MOST_QUALIFIED_USER + ";q=1.0");
         headerValues.add("that other user;q=0.5");
         headerValues.add("127.0.0.1;q=0.1");

         when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenReturn(Collections.enumeration(headerValues));

         String user = helper.getPreferredUser(mockedRequest);
         assertEquals("Helper must understand user header quality", MOST_QUALIFIED_USER, user);
      }

      @Test
      public void shouldReturnFirstUserInListForUsersWithoutQualityFactorsPresent() {
         List<String> headerValues = new LinkedList<String>();
         headerValues.add(MOST_QUALIFIED_USER);
         headerValues.add("that other user");
         headerValues.add("127.0.0.1");

         when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenReturn(Collections.enumeration(headerValues));

         String user = helper.getPreferredUser(mockedRequest);
         assertEquals("Helper must return correct user without quality factors", MOST_QUALIFIED_USER, user);
      }
   }

   public static class WhenGettingPreferredGroup {

      private static final String MOST_QUALIFIED_GROUP = "the best group of them all";
      private final RateLimitingServiceHelper helper = new RateLimitingServiceHelper(null, null, null);
      protected HttpServletRequest mockedRequest;

      @Before
      public void standUp() {
         mockedRequest = mock(HttpServletRequest.class);
      }

      @Test
      public void shouldReturnMostQualifiedGroups() {
         final List<String> headerNames = new LinkedList<String>();
         headerNames.add(PowerApiHeader.GROUPS.toString());

         when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));

         List<String> headerValues = new LinkedList<String>();
         headerValues.add("group-4;q=0.1");
         headerValues.add("group-2;q=0.1");
         headerValues.add("group-1;q=0.1");
         headerValues.add("group-3;q=0.002");

         when(mockedRequest.getHeaders(PowerApiHeader.GROUPS.toString())).thenReturn(Collections.enumeration(headerValues));

         List<String> groups = helper.getPreferredGroups(mockedRequest);

         List<String> expected = new LinkedList<String>();
         expected.add("group-4");
         expected.add("group-2");
         expected.add("group-1");

         assertEquals("Helper must understand group quality", expected, groups);
      }

      @Test
      public void shouldReturnEmptyGroupListWhenNoGroupsHeaderIsPresent() {
         List<String> groups = helper.getPreferredGroups(mockedRequest);

         assertEquals("Helper must return empty list for groups when no group information is present in the request", 0, groups.size());
      }

      @Test
      public void shouldReturnAllGroupsWhenQualityFactorsNotPresent() {
         final List<String> headerNames = new LinkedList<String>();
         headerNames.add(PowerApiHeader.GROUPS.toString());

         when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));

         List<String> headerValues = new LinkedList<String>();
         headerValues.add(MOST_QUALIFIED_GROUP);
         headerValues.add("group-4");
         headerValues.add("group-2");
         headerValues.add("group-1");
         headerValues.add("group-3");

         when(mockedRequest.getHeaders(PowerApiHeader.GROUPS.toString())).thenReturn(Collections.enumeration(headerValues));

         List<String> groups = helper.getPreferredGroups(mockedRequest);

         List<String> expected = new LinkedList<String>();
         expected.add(MOST_QUALIFIED_GROUP);
         expected.add("group-4");
         expected.add("group-2");
         expected.add("group-1");
         expected.add("group-3");

         assertEquals("Helper must return null for groups when no group information is present in the request", expected, groups);
      }
   }


}
