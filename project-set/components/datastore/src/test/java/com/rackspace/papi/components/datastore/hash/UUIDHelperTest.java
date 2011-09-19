package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.commons.util.arrays.ByteArrayComparator;
import java.util.UUID;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

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
            System.out.println(uuid.toString());
            
            final byte[] actualBytes = UUIDHelper.stringToUUIDBytes(uuid.toString());
            
            assertTrue(new ByteArrayComparator(expectedBytes, actualBytes).arraysAreEqual());
        }
    }
}
