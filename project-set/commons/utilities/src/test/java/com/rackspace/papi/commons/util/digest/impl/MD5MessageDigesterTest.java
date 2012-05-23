package com.rackspace.papi.commons.util.digest.impl;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class MD5MessageDigesterTest {

    private static final String SOURCE_DATA = "Lorem ipsum dolor sit amet";

    public static class WhenDigestingStreams {

        @Test
        public void shouldProduceCorrectDigest() {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(SOURCE_DATA.getBytes());

            final String expectedHex = "fea80f2db003d4ebc4536023814aa885";
            final String actualHex = HexHelper.convertToHex(new MD5MessageDigester().digestStream(inputStream));

            assertEquals("Digesting stream should produce expected MD5 hash",
                    expectedHex, actualHex);
        }
    }

    public static class WhenDigestingByteArrays {

        @Test
        public void shouldProduceCorrectDigest() {
            byte[] bytes = SOURCE_DATA.getBytes();

            final String expectedHex = "fea80f2db003d4ebc4536023814aa885";
            final String actualHex = HexHelper.convertToHex(new MD5MessageDigester().digestBytes(bytes));

            assertEquals("Digesting stream should produce expected MD5 hash",
                    expectedHex, actualHex);
        }
    }
}
