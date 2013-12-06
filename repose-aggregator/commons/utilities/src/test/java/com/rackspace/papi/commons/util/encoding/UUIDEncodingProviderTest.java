package com.rackspace.papi.commons.util.encoding;

import com.rackspace.papi.commons.util.arrays.ByteArrayComparator;
import org.junit.Test;

import java.security.MessageDigest;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UUIDEncodingProviderTest {

    @Test
    public void shouldEncodeMD5HashValues() throws Exception {
        final String expectedUuidValue = "cecda330-5a61-26cd-1a71-d5fe34a8e302";
        final byte[] hashBytes = MessageDigest.getInstance("MD5").digest("object-key".getBytes());

        assertEquals("UUID generated must match expected value", expectedUuidValue, UUIDEncodingProvider.getInstance().encode(hashBytes));
    }

    @Test
    public void shouldConvertWellFormedUUIDStrings() {
        final byte[] expectedBytes = new byte[16];

        for (int i = 0; i < expectedBytes.length; i++) {
            expectedBytes[i] = 1;
        }

        final UUID uuid = UUID.fromString(UUIDEncodingProvider.getInstance().encode(expectedBytes));
        final byte[] actualBytes = UUIDEncodingProvider.getInstance().decode(uuid.toString());

        assertTrue(new ByteArrayComparator(expectedBytes, actualBytes).arraysAreEqual());
    }
}
