package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.times;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Enclosed.class)
public class ByteBufferInputStreamTest {

   public static class WhenStreamIsClosed {

      private ByteBuffer buffer;
      private ByteBufferInputStream stream;

      @Before
      public void setUp() throws IOException {
         buffer = mock(ByteBuffer.class);

         when(buffer.available()).thenReturn(10);

         stream = new ByteBufferInputStream(buffer);
         stream.close();
      }

      @Test(expected = IOException.class)
      public void shouldThrowExceptionWhenReading() throws IOException {
         stream.read();
      }

      @Test(expected = IOException.class)
      public void shouldThrowExceptionWhenReadingByteBuffer() throws IOException {
         byte[] bytes = new byte[0];
         stream.read(bytes);
      }

      @Test(expected = IOException.class)
      public void shouldThrowExceptionWhenReadingByteBufferWithOffset() throws IOException {
         byte[] bytes = new byte[0];
         stream.read(bytes, 0, 10);
      }

      @Test(expected = IOException.class)
      public void shouldThrowExceptionWhenSkipping() throws IOException {
         stream.skip(0);
      }

      @Test(expected = IOException.class)
      public void shouldThrowExceptionWhenCheckingAvailable() throws IOException {
         stream.available();
      }

      @Test(expected = IOException.class)
      public void shouldThrowExceptionWhenClosing() throws IOException {
         stream.close();
      }
   }

   public static class WhenSkippingData {

      private ByteBuffer buffer;
      private ByteBufferInputStream stream;
      private static final int MAGIC_SKIP = 42;

      private static class CustomSkipAnswer implements Answer {

         @Override
         public Object answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            for (Object arg : args) {
               if (arg instanceof Integer) {
                  int value = (Integer) arg;
                  if (value == MAGIC_SKIP) {
                     // magic skip value tells buffer to not skip any values
                     return 0;
                  }
                  if (value > 10 && value <= 100) {
                     // skip in blocks of 10 when skip value <= 100
                     return 10;
                  }
                  return value;
               }
            }

            return null;
         }
      }

      @Before
      public void setUp() {
         buffer = mock(ByteBuffer.class);

         when(buffer.available()).thenReturn(10);
         when(buffer.skip(anyInt())).thenAnswer(new CustomSkipAnswer());

         stream = new ByteBufferInputStream(buffer);
      }

      @Test
      public void shouldIgnoreZeroSkip() throws IOException {
         int expected = 0;
         assertEquals(expected, stream.skip(0));
         verify(buffer, times(0)).skip(anyInt());
      }

      @Test
      public void shouldIgnoreNegativeSkip() throws IOException {
         int expected = 0;
         assertEquals(expected, stream.skip(-10));
         verify(buffer, times(0)).skip(anyInt());
      }

      @Test
      public void shouldExitIfZeroSkipped() throws IOException {
         int expected = 0;
         assertEquals(expected, stream.skip(MAGIC_SKIP));
         verify(buffer, times(1)).skip(anyInt());
      }

      @Test
      public void shouldPassSkipValueToBuffer() throws IOException {
         int expected = 10;

         stream.skip(expected);

         verify(buffer).skip(expected);
      }

      @Test
      public void shouldCallSkipUntilTotalBytesSkipped() throws IOException {
         int expected = 100;

         stream.skip(expected);

         verify(buffer, times(10)).skip(anyInt());
      }

      @Test
      public void shouldNotSkipMoreThanMaxIntBytesAtOnce() throws IOException {

         long expected = 3 * new Long(Integer.MAX_VALUE) + 1;
         long actual = stream.skip(expected);

         assertEquals("Should skip requested number of bytes", expected, actual);
         verify(buffer, times(4)).skip(anyInt());
      }
   }

   @Ignore
   public static class IsByteArray extends ArgumentMatcher<byte[]> {

      @Override
      public boolean matches(Object argument) {
         boolean result = (argument instanceof byte[]);

         return result;
      }
   }

   public static byte[] anyByteArray() {
      return argThat(new IsByteArray());
   }

   @Ignore
   public static class ByteReadAnswer implements Answer {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {

         // Fill the byte array with integers based on the index 
         int count = 0;
         Object[] args = invocation.getArguments();
         for (Object arg : args) {
            if (arg instanceof byte[]) {
               byte[] bytes = (byte[]) arg;
               for (int i = 0; i < bytes.length; i++) {
                  bytes[i] = (byte) i;
                  count++;
               }
            }
         }
         return new Integer(count);
      }
   }

   public static class WhenReadingData {

      private ByteBuffer buffer;
      private ByteBufferInputStream stream;

      @Before
      public void setUp() throws IOException {
         buffer = mock(ByteBuffer.class);

         when(buffer.available()).thenReturn(10);
         when(buffer.get()).thenReturn((byte) 1);
         when(buffer.get(anyByteArray())).thenAnswer(new ByteReadAnswer());
         when(buffer.get(anyByteArray(), anyInt(), anyInt())).thenAnswer(new ByteReadAnswer());

         stream = new ByteBufferInputStream(buffer);
      }

      @Test
      public void shouldStopReadingOnNoBytesRemaining() throws Exception {
         final byte[] bytes = new byte[]{1, 2, 3};
         ByteBuffer localBuffer = new CyclicByteBuffer();
         localBuffer.put(bytes);

         stream = new ByteBufferInputStream(localBuffer.copy());

         for (int c = 0; c < 4; c++) {
            if (c == 3) {
               assertEquals(-1, stream.read());
            } else {
               assertEquals(bytes[c], stream.read());
            }
         }
      }

      @Test
      public void shouldNotSupportMark() {
         assertFalse("Should not support marking", stream.markSupported());
      }

      @Test
      public void shouldReturnAvailableIfSourceHasDataAvailable() throws IOException {
         int expected = 10;

         assertEquals("Should have data available", expected, stream.available());
         verify(buffer).available();
      }

      @Test
      public void shouldReturnOneByteWhenReading() throws IOException {
         byte expected = 1;
         assertEquals(expected, stream.read());
         verify(buffer).get();
      }

      private void checkArrayValues(byte[] array) {
         for (int i = 0; i < array.length; i++) {
            assertEquals((byte) i, array[i]);
         }
      }

      @Test
      public void shouldFillByteArray() throws IOException {
         byte[] array = new byte[5];
         assertEquals(array.length, stream.read(array));

         checkArrayValues(array);

         verify(buffer).get(anyByteArray());
      }

      @Test
      public void shouldPassOffsetAndLength() throws IOException {
         byte[] array = new byte[5];
         assertEquals(array.length, stream.read(array, 2, 5));

         checkArrayValues(array);

         verify(buffer).get(anyByteArray(), eq(2), eq(5));
      }
   }

   /**
    * Test of read method, of class ByteBufferInputStream.
    */
   @Test
   public void testRead_byteArr() throws Exception {
      System.out.println("read");
      byte[] b = null;
      ByteBufferInputStream instance = null;
      int expResult = 0;
      int result = instance.read(b);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of read method, of class ByteBufferInputStream.
    */
   @Test
   public void testRead_3args() throws Exception {
      System.out.println("read");
      byte[] b = null;
      int off = 0;
      int len = 0;
      ByteBufferInputStream instance = null;
      int expResult = 0;
      int result = instance.read(b, off, len);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of skip method, of class ByteBufferInputStream.
    */
   @Test
   public void testSkip() throws Exception {
      System.out.println("skip");
      long n = 0L;
      ByteBufferInputStream instance = null;
      long expResult = 0L;
      long result = instance.skip(n);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }
}
