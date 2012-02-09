package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import com.rackspace.papi.components.identity.header.config.HttpHeaderList;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class HeaderIdentityHandlerTest {

   public static class WhenHandlingRequests {
      private static String IP_HEADER_NAME_1 = "IP1";
      private static String IP_HEADER_NAME_2 = "IP2";
      private static String IP_HEADER_1 = "127.0.0.1";
      private static String IP_HEADER_2 = "127.0.0.2";
      private static String DEFAULT_IP_VALUE = "10.0.0.1";
      private static String QUALITY = "0.2";
      private static String QUALITY_VALUE = ";q=0.2";
      private HttpServletRequest request;
      private ReadableHttpServletResponse response;
      private HeaderIdentityHandler handler;
      private HeaderIdentityConfig config;

      @Before
      public void setUp() {
         HttpHeaderList headerList = new HttpHeaderList();

         // Tell the handler to look for two headers called IP1 and IP2
         config = new HeaderIdentityConfig();
         config.setQuality(QUALITY);
         
         HttpHeader header = new HttpHeader();
         header.setId(IP_HEADER_NAME_1);
         headerList.getHeader().add(header);
         header = new HttpHeader();
         header.setId(IP_HEADER_NAME_2);
         headerList.getHeader().add(header);
         config.setSourceHeaders(headerList);
         
         handler = new HeaderIdentityHandler(config, QUALITY_VALUE);
         request = mock(HttpServletRequest.class);
         response = mock(ReadableHttpServletResponse.class);
         
         when(request.getHeader(IP_HEADER_NAME_1)).thenReturn(IP_HEADER_1);
         when(request.getHeader(IP_HEADER_NAME_2)).thenReturn(IP_HEADER_2);
         when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
      }

      @Test
      public void shouldSetTheUserHeaderToTheHeaderIpValue() {
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());
         
         String ip = values.iterator().next();
         
         assertEquals("Should find IP address in header", IP_HEADER_1 + QUALITY_VALUE, ip);
         
         Set<String> groups = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.GROUPS.toString().toLowerCase());
         assertFalse("Should have " + PowerApiHeader.GROUPS.toString() + " header set.", groups.isEmpty());
         
         String group = groups.iterator().next();
         assertEquals("Should find Group name in header", IP_HEADER_NAME_1 , group);
      }


      @Test
      public void shouldSetTheUserHeaderToThe2ndHeaderIpValue() {
         // Let's "erase" the first IP header value
         when(request.getHeader(IP_HEADER_NAME_1)).thenReturn(null);

         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());
         
         String ip = values.iterator().next();
         
         assertEquals("Should find 2nd IP address in header", IP_HEADER_2 + QUALITY_VALUE, ip);
      }

      @Ignore
      public void shouldSetTheUserHeaderToTheDefaultIpValue() {
         config.getSourceHeaders().getHeader().clear();
         
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());
         assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values.isEmpty());
         
         String ip = values.iterator().next();
         
         assertEquals("Should have the default IP address", DEFAULT_IP_VALUE + QUALITY_VALUE, ip);
      }
   }
}
