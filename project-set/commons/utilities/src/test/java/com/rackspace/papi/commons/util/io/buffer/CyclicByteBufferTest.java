package com.rackspace.papi.commons.util.io.buffer;

import com.rackspace.papi.commons.util.arrays.ByteArrayComparator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class CyclicByteBufferTest {

   private static final int DEFAULT_SIZE = 2048;

   private static byte[] fill(byte[] array) {
      for (int i = 0; i < array.length; i++) {
         array[i] = (byte) (i % Byte.MAX_VALUE);
      }

      return array;
   }

   private static boolean compare(byte[] array1, byte[] array2) {
      if (array1.length != array2.length) {
         return false;
      }

      boolean result = true;
      for (int i = 0; i < array1.length; i++) {
         result &= array1[i] == array2[i];
      }

      return result;
   }

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
      public void shouldSkipToEndOfData() throws IOException {
         byte[] data = fill(new byte[10]);
         buffer.put(data);
         int expected = data.length;
         int actual = buffer.skip(DEFAULT_SIZE);

         assertEquals("Should skip to end of buffer", expected, actual);
      }

      @Test
      public void shouldWrapSkip() throws IOException {
         buffer = new CyclicByteBuffer(HeapspaceByteArrayProvider.getInstance(), 10, 0, 0, false, false);
         byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7};

         // Shift buffer contents
         buffer.put(data);
         buffer.get(data);

         assertEquals("Should have 10 remaining", 10, buffer.remaining());

         buffer.put(data);
         assertEquals("Should have 3 remaining", 3, buffer.remaining());

         buffer.skip(6);

         assertEquals(7, buffer.get());
      }
   }

   public static class WhenPutting {

      private CyclicByteBuffer buffer;

      @Before
      public void standUp() {
         buffer = new CyclicByteBuffer(HeapspaceByteArrayProvider.getInstance());
      }

      @Test
      public void shouldHaveDefaultBufferSizeAvailable() {
         buffer.allocate();
         assertEquals("Should have default buffer size", DEFAULT_SIZE, buffer.remaining());
      }

      @Test
      public void shouldPutIntoBuffer() throws IOException {
         final byte expected = 0x1;

         buffer.put(expected);
         assertEquals("Byte in buffer should be same as the byte put into the buffer", expected, buffer.get());
      }

      @Test
      public void shouldGrowBuffer() throws IOException {
         final int expectedAvailable = 2048;

         buffer.put(new byte[6]);
         buffer.get(new byte[6]);

         assertEquals("Buffer size should should have 2048 available after growing", expectedAvailable, buffer.remaining());
      }

      @Test
      public void shouldGrowBufferWhenExactlyFull() throws IOException {
         final int expectedAvailable = 2048;

         buffer.put((byte) 0x01);
         buffer.put((byte) 0x02);
         buffer.put((byte) 0x03);
         buffer.put((byte) 0x04);
         buffer.put((byte) 0x05);
         buffer.get(new byte[5]);

         assertEquals("Buffer size should should have 2048 available after growing", expectedAvailable, buffer.remaining());
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

      @Test
      public void shouldWrapPutsAndPreserveOrder() throws IOException {
         byte[] data1Written = new byte[10];
         buffer.put(fill(data1Written));

         assertEquals("Should reduce remaining", DEFAULT_SIZE - data1Written.length, buffer.remaining());

         byte[] data1Read = new byte[10];
         buffer.get(data1Read);

         assertEquals("Should have full space remaining", DEFAULT_SIZE, buffer.remaining());

         byte[] data2Written = new byte[DEFAULT_SIZE];
         buffer.put(fill(data2Written));

         assertEquals("Should have zero remaining", 0, buffer.remaining());
         byte[] data2Read = new byte[DEFAULT_SIZE];

         buffer.get(data2Read);

         assertTrue(compare(data2Written, data2Read));
      }

      @Test
      public void shouldWrapPuts() throws IOException {
         buffer = new CyclicByteBuffer(HeapspaceByteArrayProvider.getInstance(), 10, 0, 0, false, false);
         byte[] data = fill(new byte[7]);

         // Shift buffer contents
         buffer.put(data);
         buffer.get(data);

         assertEquals("Should have 10 remaining", 10, buffer.remaining());

         buffer.put(data);
         assertEquals("Should have 3 remaining", 3, buffer.remaining());

         byte[] read = new byte[7];
         buffer.get(read);

         assertTrue(compare(data, read));
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

   public static class WhenClearing {

      private CyclicByteBuffer sourceBuffer;

      @Before
      public void standUp() {
         sourceBuffer = new CyclicByteBuffer();
      }

      @Test
      public void shouldClearEmptyBuffer() {
         int expected = sourceBuffer.remaining();
         sourceBuffer.clear();
         int actual = sourceBuffer.remaining();
         assertEquals("Remaining should be unchanged after clearing empty buffer", expected, actual);
      }

      @Test
      public void shouldClearPartiallyFullBuffer() throws IOException {
         int expected = sourceBuffer.remaining();
         sourceBuffer.put(fill(new byte[20]));
         sourceBuffer.clear();
         int actual = sourceBuffer.remaining();
         assertTrue("Remaining should be entire buffer after clearing", actual > 0);
      }
   }

   public static class WhenCopying {

      private CyclicByteBuffer sourceBuffer;

      @Before
      public void standUp() {
         sourceBuffer = new CyclicByteBuffer();
      }

      @Test
      public void shouldCopyBuffer() throws Exception {
         ByteBuffer dest = (ByteBuffer) sourceBuffer.clone();
         
         assertNotNull(dest);
         assertEquals("Dest and source available should be the same", sourceBuffer.available(), dest.available());
         assertEquals("Dest and source remaining should be the same", sourceBuffer.remaining(), dest.remaining());
      }

      @Test
      public void shouldPreserveData() throws IOException {
         byte[] dataWritten = fill(new byte[10]);
         sourceBuffer.put(dataWritten);

         ByteBuffer dest = sourceBuffer.copy();
         byte[] dataRead = new byte[dataWritten.length];
         dest.get(dataRead);

         assertTrue("Data read from clone should match data written to source", compare(dataRead, dataWritten));

      }

      @Test
      public void shouldAllocateSufficientBuffer() throws IOException {
         byte[] dataWritten = fill(new byte[DEFAULT_SIZE + 10]);
         sourceBuffer.put(dataWritten);

         ByteBuffer dest = sourceBuffer.copy();
         byte[] dataRead = new byte[dataWritten.length];
         dest.get(dataRead);

         assertTrue("Data read from clone should match data written to source", compare(dataRead, dataWritten));

      }

      @Test
      public void shouldCopyWrappedBuffers() throws IOException {
         byte[] dataWritten = fill(new byte[DEFAULT_SIZE]);
         // shift internal buffers by 10
         sourceBuffer.put(new byte[10]);
         sourceBuffer.get(new byte[10]);

         // fill buffer.  Data will be "wrapped" in internal buffer
         sourceBuffer.put(dataWritten);

         ByteBuffer dest = sourceBuffer.copy();
         byte[] dataRead = new byte[dataWritten.length];
         dest.get(dataRead);

         assertTrue("Data read from clone should match data written to source", compare(dataRead, dataWritten));

      }
   }

   public static class Regression {

      private CyclicByteBuffer sourceBuffer;

      @Before
      public void standUp() {
         sourceBuffer = new CyclicByteBuffer();
      }

      @Test
      public void shouldReadFromFullBuffer() throws IOException {
         int expected = 2048;
         sourceBuffer.put(fill(new byte[2048]));
         assertEquals("Available should be entire buffer", expected, sourceBuffer.available());
         
         byte[] buffer = new byte[2048];
         sourceBuffer.get(buffer);
         
         assertEquals("Available should be zero after draining", 0, sourceBuffer.available());
      }
   }

}
