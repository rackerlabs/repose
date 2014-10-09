/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.headers.request;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;


@RunWith(Enclosed.class)
public class ViaRequestHeaderBuilderTest {

   @Ignore
   public static abstract class TestParent {

      protected ViaRequestHeaderBuilder builder;
      protected MutableHttpServletRequest request;

      @Before
      public final void beforeAll() {
         builder = new ViaRequestHeaderBuilder("2.4.0", getVia(), getHostname());
         request = mock(MutableHttpServletRequest.class);
         
         
      }
      
      public abstract String getVia();
      
      public abstract String getHostname();
   }


   public static class WhenBuildingResponseViaHeadersWithEmptyReceivedBy extends TestParent{
      
      @Override
      public String getVia(){
         return "";
      }
      
      @Override
      public String getHostname(){
         return "ReposeHost";
      }
      
      @Before
      public void standUp(){
         
         when(request.getProtocol()).thenReturn("HTTP/1.1");
         when(request.getLocalPort()).thenReturn(8888);
      }
      
      @Test
      public void shouldGenerateViaHeaderWithStandardValues(){
         String via = builder.buildVia(request);
         
         assertEquals("1.1 ReposeHost:8888 (Repose/2.4.0)", via);
      }
   }
   
   public static class WhenBuildingResponseViaHeadersWithReceivedBy extends TestParent{
      
      @Override
      public String getVia(){
         return "ConfiguredViaValue";
      }
      
      @Override
      public String getHostname(){
         return "ReposeHost";
      }
      
      @Before
      public void standUp(){
         
         when(request.getProtocol()).thenReturn("HTTP/1.1");
         when(request.getLocalPort()).thenReturn(8888);
      }
      
      @Test
      public void shouldGenerateViaHeaderWithStandardValues(){
         String via = builder.buildVia(request);
         
         assertEquals("1.1 ConfiguredViaValue (Repose/2.4.0)", via);
      }
   }
   
}
