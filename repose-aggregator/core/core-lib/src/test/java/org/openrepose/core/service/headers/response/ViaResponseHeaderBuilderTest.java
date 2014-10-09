/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.core.service.headers.response;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

/**
 *
 * @author malconis
 */
@RunWith(Enclosed.class)
public class ViaResponseHeaderBuilderTest {

   @Ignore
   public static abstract class TestParent {

      protected ViaResponseHeaderBuilder builder;
      protected MutableHttpServletRequest request;

      @Before
      public final void beforeAll() {
         builder = new ViaResponseHeaderBuilder("2.4.0", getVia());
         request = mock(MutableHttpServletRequest.class);
         
         
      }
      
      public abstract String getVia();
   }

   @Before
   public void setUp() {

      

   }

   public static class WhenBuildingResponseViaHeadersWithEmptyReceivedBy extends TestParent{
      
      @Override
      public String getVia(){
         return "";
      }
      
      @Before
      public void standUp(){
         
         when(request.getProtocol()).thenReturn("HTTP/1.1");
      }
      
      @Test
      public void shouldGenerateViaHeaderWithStandardValues(){
         String via = builder.buildVia(request);
         
         assertEquals("1.1 Repose (Repose/2.4.0)", via);
      }
   }
   
   public static class WhenBuildingResponseViaHeadersWithReceivedBy extends TestParent{
      
      @Override
      public String getVia(){
         return "ReposeTest";
      }
      
      @Before
      public void standUp(){
         
         when(request.getProtocol()).thenReturn("HTTP/1.1");
      }
      
      @Test
      public void shouldGenerateViaHeaderWithStandardValues(){
         String via = builder.buildVia(request);
         
         assertEquals("1.1 ReposeTest (Repose/2.4.0)", via);
      }
   }
   
   public static class WhenBuildingResponseViaHeadersWithEmptyResponseProtocolAndReceivedBy extends TestParent{
      
      @Override
      public String getVia(){
         return "ReposeTest";
      }
      
      @Before
      public void standUp(){
         
         when(request.getProtocol()).thenReturn("");
      }
      
      @Ignore
      @Test
      public void shouldGenerateViaHeaderWithStandardValues(){
         String via = builder.buildVia(request);
         
         assertEquals("1.1 ReposeTest (Repose/2.4.0)", via);
      }
   }
}
