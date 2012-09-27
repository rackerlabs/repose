package com.rackspace.papi.commons.util.http.media;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class MediaTypeTest {

   public static class WhenCheckingIfEqual {

      @Test
      public void shouldReturnTrueIfComparingEqualTypes() {
         MimeType mediaType = MimeType.getMatchingMimeType("application/vnd.rackspace.services.a-v1.0+xml");
         MediaType oneMediaRange = new MediaType("application/vnd.rackspace.services.a-v1.0+xml", mediaType, -1);
         MediaType twoMediaRange = new MediaType("application/vnd.rackspace.services.a-v1.0+xml", mediaType, -1);

         assertTrue(oneMediaRange.equals(twoMediaRange));
      }

      @Test
      public void shouldReturnFalseIfComparingADifferentType() {
         MimeType mediaType = MimeType.getMatchingMimeType("application/vnd.rackspace.services.a-v1.0+xml");
         MediaType oneMediaRange = new MediaType("application/vnd.rackspace.services.a-v1.0+xml", mediaType, -1);

         assertFalse(oneMediaRange.equals("another object"));
      }
   }
}
