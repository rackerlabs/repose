package com.rackspace.papi.components.identity.header.extractor;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class ClientIpExtractorTest {

   public static class WhenExtractingIpAddresses {
      private HttpServletRequest request;
      private static String IP_HEADER_NAME = "IP";
      private static String INVALID_IP_HEADER_NAME = "INVALID_IP";
      private static String MULTIPLE_IP_HEADER_NAME = "MULTIPLE IPS";
      private static String MULTIPLE_IPS = "1.1.1.1,2.2.2.2,3.3.3.3";
      private static String NON_EXISTENT_HEADER = "Some other header";
      private static String IP_HEADER = "127.0.0.1";
      private static String INVALID_IP = "unknown";
      private static String DEFAULT_IP_VALUE = "10.0.0.1";
      private static String DEFAULT_QUALITY_VALUE = ";q=0.1";
      private HeaderValueExtractor extractor;

      @Before
      public void setUp() {
         request = mock(HttpServletRequest.class);
         extractor = new HeaderValueExtractor(request);
         
         when(request.getHeader(IP_HEADER_NAME)).thenReturn(IP_HEADER);
         when(request.getHeader(INVALID_IP_HEADER_NAME)).thenReturn(INVALID_IP);
         when(request.getHeader(MULTIPLE_IP_HEADER_NAME)).thenReturn(MULTIPLE_IPS);
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
      public void shouldGetHeaderIpAddress() {
         List<HttpHeader> headers = new ArrayList<HttpHeader>();
         HttpHeader header = new HttpHeader();
         header.setId(IP_HEADER_NAME);
         headers.add(header);
         
         List<ExtractorResult<String>> act = extractor.extractUserGroup(headers);
         
         assertEquals("Should find Header User", IP_HEADER + DEFAULT_QUALITY_VALUE, act.get(0).getResult());
         assertEquals("Should find Header Group", IP_HEADER_NAME + DEFAULT_QUALITY_VALUE, act.get(0).getKey());
      }
      
      @Test
      public void shouldGetFirstIpFromList() {
         final String expected = "1.1.1.1"; 
         List<HttpHeader> headers = new ArrayList<HttpHeader>();
         HttpHeader header = new HttpHeader();
         header.setId(MULTIPLE_IP_HEADER_NAME);
         headers.add(header);
         
         List<ExtractorResult<String>> act = extractor.extractUserGroup(headers);
         assertEquals("Should find Header User", expected + DEFAULT_QUALITY_VALUE, act.get(0).getResult());
         assertEquals("Should find Header Group", MULTIPLE_IP_HEADER_NAME + DEFAULT_QUALITY_VALUE, act.get(0).getKey());
         
      }
   }

}
