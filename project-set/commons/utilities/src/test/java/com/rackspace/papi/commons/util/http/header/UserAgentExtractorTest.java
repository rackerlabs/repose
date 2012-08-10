/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.http.header;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import javax.servlet.http.HttpServletRequest;
import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author malconis
 */
@RunWith(Enclosed.class)
public class UserAgentExtractorTest {

   public static class WhenExtractingUserAgent {

      private HttpServletRequest request;
      private UserAgentExtractor extractor;
      private String USER_AGENT = "user-agent";
      private String VIA = "via";
      private String curlUserAgent = "curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15";
      private String viaHeader = "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy";

      @Before
      public void setUp() {
         request = mock(HttpServletRequest.class);
         extractor = new UserAgentExtractor(request);
         when(request.getHeader(USER_AGENT)).thenReturn(curlUserAgent);
         when(request.getHeader(VIA)).thenReturn(viaHeader);

      }

      @Test
      public void shouldExtractUserAgent() {
         String result = extractor.extractAgent(USER_AGENT);

         assertTrue(result.equals(curlUserAgent));
      }

      @Test
      public void shouldExtractAgentInfo() {

         ExtractorResult<String> result = extractor.extractAgentInfo(curlUserAgent);

         assertTrue(result.getResult().equals("curl"));
         assertTrue(result.getKey().equals("7.19.7"));
      }

      @Test
      public void shouldExtractLastVia() {
         String result = extractor.extractAgent(VIA);
         
         assertTrue(result.equals("1.0 lucy"));
      }
   }
}
