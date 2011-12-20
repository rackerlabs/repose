package com.rackspace.papi.components.clientip;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientip.config.ClientIpIdentityConfig;
import com.rackspace.papi.components.clientip.config.HttpHeader;
import com.rackspace.papi.components.clientip.config.HttpHeaderList;
import com.rackspace.papi.components.clientip.extractor.ClientIpExtractor;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ClientIpIdentityHandlerTest {

   public static class WhenHandlingRequests {
      private static String IP_HEADER_NAME_1 = "IP1";
      private static String IP_HEADER_NAME_2 = "IP2";
      private static String IP_HEADER_1 = "127.0.0.1";
      private static String IP_HEADER_2 = "127.0.0.2";
      private static String DEFAULT_IP_VALUE = "10.0.0.1";
      private HttpServletRequest request;
      private ReadableHttpServletResponse response;
      private ClientIpIdentityHandler handler;
      private ClientIpIdentityConfig config;

      @Before
      public void setUp() {
         HttpHeaderList headerList = new HttpHeaderList();

         // Tell the handler to look for two headers called IP1 and IP2
         config = new ClientIpIdentityConfig();
         HttpHeader header = new HttpHeader();
         header.setId(IP_HEADER_NAME_1);
         headerList.getHeader().add(header);
         header = new HttpHeader();
         header.setId(IP_HEADER_NAME_2);
         headerList.getHeader().add(header);
         config.setSourceHeaders(headerList);
         
         handler = new ClientIpIdentityHandler(config);
         request = mock(HttpServletRequest.class);
         response = mock(ReadableHttpServletResponse.class);
         
         when(request.getHeader(IP_HEADER_NAME_1)).thenReturn(IP_HEADER_1);
         when(request.getHeader(IP_HEADER_NAME_2)).thenReturn(IP_HEADER_2);
         when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
      }

      @Test
      public void shouldSetTheUserHeaderToTheHeaderIpValue() {
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(ClientIpIdentityHandler.DEST_HEADER.toLowerCase());
         assertFalse("Should have " + ClientIpIdentityHandler.DEST_HEADER + " header set.", values.isEmpty());
         
         String ip = values.iterator().next();
         
         assertEquals("Should find IP address in header", IP_HEADER_1, ip);
      }

      @Test
      public void shouldSetTheUserHeaderToThe2ndHeaderIpValue() {
         // Let's "erase" the first IP header value
         when(request.getHeader(IP_HEADER_NAME_1)).thenReturn(null);

         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(ClientIpIdentityHandler.DEST_HEADER.toLowerCase());
         assertFalse("Should have " + ClientIpIdentityHandler.DEST_HEADER + " header set.", values.isEmpty());
         
         String ip = values.iterator().next();
         
         assertEquals("Should find 2nd IP address in header", IP_HEADER_2, ip);
      }

      @Test
      public void shouldSetTheUserHeaderToTheDefaultIpValue() {
         config.getSourceHeaders().getHeader().clear();
         
         FilterDirector result = handler.handleRequest(request, response);
         
         Set<String> values = result.requestHeaderManager().headersToAdd().get(ClientIpIdentityHandler.DEST_HEADER.toLowerCase());
         assertFalse("Should have " + ClientIpIdentityHandler.DEST_HEADER + " header set.", values.isEmpty());
         
         String ip = values.iterator().next();
         
         assertEquals("Should have the default IP address", DEFAULT_IP_VALUE, ip);
      }
   }
}
