package com.rackspace.papi.commons.util.io.buffer;

import com.rackspace.papi.commons.util.arrays.ByteArrayComparator;
import org.junit.Before;
import java.io.IOException;
import java.util.Random;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class CyclicByteBufferTest {

    public static class WhenSkipping {

        private CyclicByteBuffer buffer;

        @Before
        public void standUp() {
            buffer = new CyclicByteBuffer();
        }

        @Test
        public void shouldSkipBytes() throws IOException {
            buffer.put((byte) 1);
            buffer.put(new byte[256]);
            buffer.put((byte) 100);

            assertEquals("Header byte before skipping must mach expected", 1, buffer.get());
            buffer.skip(256);
            assertEquals("Trailing byte after skipping must match expected", 100, buffer.get());
        }

        @Test
        public void shouldAllBytes() throws IOException {
            buffer.put(new byte[256]);

            buffer.skip(256);
            assertEquals("Trailing byte after skipping must match expected", 100, buffer.get());
        }
    }

    public static class WhenPutting {

        private CyclicByteBuffer buffer;

        @Before
        public void standUp() {
            buffer = new CyclicByteBuffer(4);
        }

        @Test
        public void shouldPutIntoBuffer() throws IOException {
            final byte expected = 0x1;

            buffer.put(expected);

            assertEquals("Byte in buffer should be same as the byte put into the buffer", expected, buffer.get());
        }

        @Test
        public void shouldGrowBuffer() throws IOException {
            final int expectedAvailable = 8;

            buffer.put(new byte[6]);
            buffer.get(new byte[6]);

            assertEquals("Buffer size should should have 8 available after growing", expectedAvailable, buffer.remaining());
        }

        @Test
        public void shouldGrowBufferWhenExactlyFull() throws IOException {
            final int expectedAvailable = 8;

            buffer.put((byte) 0x01);
            buffer.put((byte) 0x02);
            buffer.put((byte) 0x03);
            buffer.put((byte) 0x04);
            buffer.put((byte) 0x05);
            buffer.get(new byte[5]);

            assertEquals("Buffer size should should have 8 available after growing", expectedAvailable, buffer.remaining());
        }

        @Test
        public void shouldGrowBufferAndPreserveByteOrder() throws IOException {
            final byte[] expectedHeader = new byte[8];

            for (int i = 0; i < expectedHeader.length; i++) {
                expectedHeader[i] = (byte) i;
            }

            buffer.put(expectedHeader);
            buffer.put(new byte[4096]);

            final byte[] retrievedHeader = new byte[8];
            buffer.get(retrievedHeader);

            for (int i = 0; i < expectedHeader.length; i++) {
                assertEquals("Growing should preserve previous data and order", expectedHeader[i], retrievedHeader[i]);
            }
        }
    }

    public static class WhenGetting {

        private CyclicByteBuffer buffer;

        @Before
        public void standUp() {
            buffer = new CyclicByteBuffer();
        }

        @Test
        public void shouldReturnReadWithEmptyBuffer() throws IOException {
            assertEquals("Buffer should return zero when a read is made against an empty buffer", 0, buffer.get(new byte[16]));
        }

        @Test
        public void shouldReturnNegativeOneForSingleByteReadsWithEmptyBuffer() throws IOException {
            assertEquals("Buffer should return negative one on empty single byte read", -1, buffer.get());
        }

        @Test
        public void shouldReadUntilSuppliedByteArrayIsFull() throws IOException {
            final int expected = 32;

            buffer.put(new byte[64]);

            assertEquals("Byte buffer should fill given array and return the amount read", expected, buffer.get(new byte[32]));
        }

        @Test
        public void shouldHonorOffsets() throws IOException {
            buffer = new CyclicByteBuffer();

            final byte[] randomBytes = new byte[64];
            new Random(System.nanoTime()).nextBytes(randomBytes);

            buffer.put(randomBytes);

            final byte[] expected = new byte[32];
            System.arraycopy(randomBytes, 0, expected, 0, 16);
            System.arraycopy(randomBytes, 32, expected, 16, 16);

            final byte[] actual = new byte[expected.length];

            buffer.get(actual, 0, 16);
            assertEquals(48, buffer.available());

            buffer.skip(16);
            assertEquals(32, buffer.available());

            buffer.get(actual, 16, 16);
            assertEquals(16, buffer.available());

            assertTrue(new ByteArrayComparator(expected, actual).arraysAreEqual());
        }
        
        @Test
        public void shouldHandleLargeArrayys() throws IOException {
            buffer = new CyclicByteBuffer();

            final byte[] randomBytes = new byte[64];
            new Random(System.nanoTime()).nextBytes(randomBytes);

            buffer.put(randomBytes);

            final byte[] expected = new byte[32];
            System.arraycopy(randomBytes, 0, expected, 0, 32);

            final byte[] readBuffer = new byte[8192];

            final int read = buffer.get(readBuffer, 0, readBuffer.length);

            assertEquals(64, read);
            
            final byte[] actual = new byte[expected.length];
            System.arraycopy(readBuffer, 0, actual, 0, actual.length);
            
            assertTrue(new ByteArrayComparator(expected, actual).arraysAreEqual());
        }
    }
}
