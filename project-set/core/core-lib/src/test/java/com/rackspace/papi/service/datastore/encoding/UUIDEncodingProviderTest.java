package com.rackspace.papi.service.datastore.encoding;

import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class UUIDEncodingProviderTest {

   public static class WhenEncodingMD5Hashes {

      @Test
      public void shouldEncodeMD5HashValues() throws Exception {
         final String expectedUuidValue = "cecda330-5a61-26cd-1a71-d5fe34a8e302";
         final byte[] hashBytes = MD5MessageDigestFactory.getInstance().newMessageDigest().digest("object-key".getBytes());
         
         assertEquals("UUID generated must match expected value", expectedUuidValue, UUIDEncodingProvider.getInstance().encode(hashBytes));
      }
   }
}
