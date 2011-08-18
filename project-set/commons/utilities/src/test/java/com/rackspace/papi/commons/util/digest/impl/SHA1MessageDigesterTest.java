package com.rackspace.papi.commons.util.digest.impl;

import com.rackspace.papi.commons.util.digest.impl.SHA1MessageDigester;
import java.io.ByteArrayInputStream;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class SHA1MessageDigesterTest {

    private static final String SOURCE_DATA = "Lorem ipsum dolor sit amet";

    public static class WhenDigestingStreams {

        @Test
        public void shouldProduceCorrectDigest() {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(SOURCE_DATA.getBytes());

            final String expectedHex = "38f00f8738e241daea6f37f6f55ae8414d7b0219";
            final String actualHex = HexHelper.convertToHex(new SHA1MessageDigester().digestStream(inputStream));

            assertEquals("Digesting stream should produce expected SHA-1 hash",
                    expectedHex, actualHex);
        }
    }

    public static class WhenDigestingByteArrays {

        @Test
        public void shouldProduceCorrectDigest() {
            byte[] bytes = SOURCE_DATA.getBytes();

            final String expectedHex = "38f00f8738e241daea6f37f6f55ae8414d7b0219";
            final String actualHex = HexHelper.convertToHex(new SHA1MessageDigester().digestBytes(bytes));

            assertEquals("Digesting stream should produce expected SHA-1 hash",
                    expectedHex, actualHex);
        }
    }
}
