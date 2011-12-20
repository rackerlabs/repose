package com.rackspace.papi.components.clientip.extractor;

import com.rackspace.papi.components.clientip.config.HttpHeader;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ClientIpExtractorTest {

   public static class WhenExtractingIpAddresses {
      private HttpServletRequest request;
      private static String IP_HEADER_NAME = "IP";
      private static String NON_EXISTENT_HEADER = "Some other header";
      private static String IP_HEADER = "127.0.0.1";
      private static String DEFAULT_IP_VALUE = "10.0.0.1";
      private ClientIpExtractor extractor;

      @Before
      public void setUp() {
         request = mock(HttpServletRequest.class);
         extractor = new ClientIpExtractor(request);
         
         when(request.getHeader(IP_HEADER_NAME)).thenReturn(IP_HEADER);
         when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
      }

      @Test
      public void shouldExtractHeader() {
         String result = extractor.extractHeader(IP_HEADER_NAME);
         assertEquals("Should find IP in header", IP_HEADER, result);
      }
      
      @Test
      public void shouldNotExtractHeader() {
         String result = extractor.extractHeader(NON_EXISTENT_HEADER);
         assertEquals("Should not find IP in invalid header", "", result);
      }
      
      @Test
      public void shouldGetDefaultIpAddress() {
         List<HttpHeader> headers = new ArrayList<HttpHeader>();
         String actual = extractor.extractIpAddress(headers);
         assertEquals("Should find default IP", DEFAULT_IP_VALUE, actual);
      }

      @Test
      public void shouldGetHeaderIpAddress() {
         List<HttpHeader> headers = new ArrayList<HttpHeader>();
         HttpHeader header = new HttpHeader();
         header.setId(IP_HEADER_NAME);
         headers.add(header);
         
         String actual = extractor.extractIpAddress(headers);
         assertEquals("Should find Header IP", IP_HEADER, actual);
      }
   }

}
