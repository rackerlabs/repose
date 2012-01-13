package com.rackspace.papi.components.clientuser;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientuser.config.ClientUserIdentityConfig;
import com.rackspace.papi.components.clientuser.config.UserMapping;
import com.rackspace.papi.components.clientuser.config.UserMappingList;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ClientUserIdentityHandlerTest {
   
   public static class WhenHandlingRequests {
      private static String QUALITY = "0.5";
      private static String QUALITY_VALUE = ";q=0.5";
      private static String URI1 = "/someuri/1234/morestuff";
      private static String REGEX1 = ".*/[^\\d]*/(\\d*)/.*";
      private static String USER1 = "1234";
      
      private static String URI2 = "/someuri/abc/someuser";
      private static String REGEX2 = ".*/[^\\d]*/abc/(.*)";
      private static String USER2 = "someuser";
      
      private static String URIFAIL = "/nouserinformation";
      
      private ClientUserIdentityConfig config;
      private HttpServletRequest request;
      private ReadableHttpServletResponse response;
      private ClientUserIdentityHandler handler;
      
      @Before
      public void setUp() {
         config = new ClientUserIdentityConfig();
         config.setQuality(QUALITY);
         
         UserMappingList userMappingList = new UserMappingList();

         UserMapping mapping = new UserMapping();
         mapping.setId("Mapping 1");
         mapping.setUserRegex(REGEX1);
         userMappingList.getMapping().add(mapping);
         
         mapping = new UserMapping();
         mapping.setId("Mapping 2");
         mapping.setUserRegex(REGEX2);
         userMappingList.getMapping().add(mapping);

         config.setUserMappings(userMappingList);
         
         handler = new ClientUserIdentityHandler(config, QUALITY_VALUE);
         request = mock(HttpServletRequest.class);
         response = mock(ReadableHttpServletResponse.class);
         
      }
      
      @Test
      public void shouldSetTheUserHeaderToTheRegexResult() {
         when(request.getRequestURI()).thenReturn(URI1);
         
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());
         
         String userName = values.iterator().next();
         
         assertEquals("Should find user name in header", USER1 + QUALITY_VALUE, userName);
      }

      @Test
      public void shouldSetTheUserHeaderToThe2ndRegexResult() {
         when(request.getRequestURI()).thenReturn(URI2);
         
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());
         
         String userName = values.iterator().next();
         
         assertEquals("Should find user name in header", USER2 + QUALITY_VALUE, userName);
      }

      @Test
      public void shouldNotHaveUserHeader() {
         when(request.getRequestURI()).thenReturn(URIFAIL);
         
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
         assertTrue("Should not have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());
         
      }
   }

}
