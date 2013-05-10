/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.hash;

import com.rackspace.papi.commons.util.arrays.ByteArrayComparator;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class UUIDHelperTest {

   public static class WhenConvertingUUIDStringsToBytes {

      @Test
      public void shouldConvertWellFormedUUIDStrings() {
         final byte[] expectedBytes = new byte[16];

         for (int i = 0; i < expectedBytes.length; i++) {
            expectedBytes[i] = 1;
         }

         final UUID uuid = UUIDHelper.bytesToUUID(expectedBytes);
         final byte[] actualBytes = UUIDHelper.stringToUUIDBytes(uuid.toString());

         assertTrue(new ByteArrayComparator(expectedBytes, actualBytes).arraysAreEqual());
      }
   }
}
