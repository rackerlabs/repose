package com.rackspace.papi.commons.util.http;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA. User: joshualockwood Date: Apr 21, 2011 Time:
 * 11:50:58 AM
 */
@RunWith(Enclosed.class)
public class CommonHttpHeaderTest {

   public static class WhenGettingHeaderKeys {

      @Test
      public void shouldReturnExpectedKey() {
         assertEquals("retry-after", CommonHttpHeader.RETRY_AFTER.toString());
      }
   }
}
