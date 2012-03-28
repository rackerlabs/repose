package com.rackspace.papi.service.rms;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class HrefFileReaderTest {

   public static class WhenValidatingHref {
      private final HrefFileReader hrefFileReader = new HrefFileReader();

      @Test
      public void shouldReturnNullIfHrefIsNotAFile() {
         assertNull(hrefFileReader.validateHref("http://something", "file_id"));
      }
   }
}
