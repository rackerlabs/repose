/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.servlet.http;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 *
 * @author malconis
 */
@RunWith(Enclosed.class)
public class RouteDestinationTest {

   public static class WhenCreatingDestinations {

      private RouteDestination routeDst1, routeDst2, routeDst3, routeDst4;

      @Before
      public void setUp() {

         routeDst1 = new RouteDestination("dst1", "/service/dst1", new Float(1.0));
         routeDst2 = new RouteDestination("dst2", "/service/dst2", new Float(-1.0));
         routeDst3 = new RouteDestination("dst3", "/service/dst3", new Float(1.0));
         routeDst4 = new RouteDestination("dst1", "/service/dst4", new Float(1.0));

      }

      @Test
      public void shouldReturnDestinationWithHighestQuality() {

         int compared = routeDst1.compareTo(routeDst2);

         assertEquals(compared, 1);
      }
      
      @Test
      public void shouldReturnDestinationWithFirstDestinationId(){
         Integer compared = routeDst1.compareTo(routeDst3);
         
         assertTrue(compared < 0);
      }
      
      @Test
      public void shouldCompareDestinationsWithUri(){
         int compared = routeDst1.compareTo(routeDst4);
         
         assertTrue(compared < 0);
      }
      
      @Test
      public void shouldHaveDifferentHashPerDestination(){
         Integer h1 = routeDst1.hashCode();
         Integer h2 = routeDst2.hashCode();
         assertFalse(h1.equals(h2));
      }
   }
}
