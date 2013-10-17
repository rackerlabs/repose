package com.rackspace.papi.commons.util.io.buffer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class SynchronizedByteBufferTest {

   public static class WhenCallingBuffer {
      private ByteBuffer b;
      private SynchronizedByteBuffer buffer;

      @Before
      public void setUp() {
         b = mock(ByteBuffer.class);
         buffer = new SynchronizedByteBuffer(b);
      }
      
      @Test
      public void shouldCallClear() {
         buffer.clear();
         verify(b).clear();
      }
      
      @Test
      public void shouldCallSkip() {
         int expected = 5;
         when(b.skip(anyInt())).thenReturn(expected);
         int len = 10;
         int actual = buffer.skip(len);
         verify(b).skip(eq(len));
         
         assertEquals(expected, actual);
      }
      
      @Test
      public void shouldCallRemaining() {
         int expected = 17;
         when(b.remaining()).thenReturn(expected);
         int actual = buffer.remaining();
         verify(b).remaining();
         assertEquals(expected, actual);
      }
      
      @Test
      public void shouldCallAvailable() {
         int expected = 42;
         when(b.available()).thenReturn(expected);
         int avail = buffer.available();
         verify(b).available();
         assertEquals(expected, avail);
      }
      
      @Test
      public void shouldCallPutByte() throws IOException {
         byte byteVal = 1;
         buffer.put(byteVal);
         verify(b).put(eq(byteVal));
      }

      @Test
      public void shouldCallPutByteArray() throws IOException {
         byte[] byteVal = {1, 2};
         buffer.put(byteVal);
         verify(b).put(eq(byteVal));
      }

      @Test
      public void shouldCallPutByteArrayWithOffset() throws IOException {
         byte[] byteVal = {1, 2};
         int off = 3;
         int len = 7;
         buffer.put(byteVal, off, len);
         verify(b).put(eq(byteVal), eq(off), eq(len));
      }
      
      @Test
      public void shouldCallGet() throws IOException {
         byte expected = 7;
         when(b.get()).thenReturn(expected);
         byte actual = buffer.get();
         
         verify(b).get();
         assertEquals(expected, actual);
      }
      
      @Test
      public void shouldCallGetBytes() throws IOException {
         byte[] someArray = new byte[10];
         buffer.get(someArray);
         
         byte[] expected = someArray;
         verify(b).get(eq(expected));
      }
      
      @Test
      public void shouldCallGetBytesWithOffset() throws IOException {
         byte[] someArray = new byte[10];
         int off = 5;
         int len = 1;
         buffer.get(someArray, off, len);
         
         byte[] expected = someArray;
         verify(b).get(eq(expected), eq(off), eq(len));
      }
      
      @Test
      public void shouldCallCopy() {
         ByteBuffer expected = mock(ByteBuffer.class);
         when(b.copy()).thenReturn(expected);
         ByteBuffer actual = buffer.copy();
         verify(b).copy();
         
         assertTrue(expected == actual);
      }
   }

}
