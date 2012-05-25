package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ByteBufferOutputStreamTest {

   public static class WhenWritingData {
      private ByteBuffer buffer;
      private ByteBufferOutputStream stream;
      private final Integer BUFFER_SIZE = 100;

      @Before
      public void setUp() {
         buffer = mock(ByteBuffer.class);
         when(buffer.available()).thenReturn(BUFFER_SIZE);
         stream = new ByteBufferOutputStream(buffer);
      }

      @After
      public void tearDown() {
      }
      
      @Test
      public void shouldWriteByte() throws IOException {
         int b = 1;
         stream.writeByte(b);
         verify(buffer).put(eq((byte)b));
      }
      
      @Test
      public void shouldFlushStream() throws IOException {
         stream.flushStream();
         verify(buffer, times(1)).skip(BUFFER_SIZE);
      }
      
      @Test
      public void shouldCallWriteByte() throws IOException {
         int b = 1;
         stream.write(b);
         verify(buffer).put(eq((byte)b));
      }

      @Test(expected=IOException.class)
      public void shouldThrowExceptionIfStreamIsClosed() throws IOException {
         byte[] b = {1};
         stream.close();
         stream.write(b);
      }
   }
   
   public static class WhenFlushingStreams {
      private ByteBuffer buffer;
      private ByteBufferOutputStream stream;
      private final Integer BUFFER_SIZE = 100;

      @Before
      public void setUp() {
         buffer = mock(ByteBuffer.class);
         when(buffer.available()).thenReturn(BUFFER_SIZE);
         stream = new ByteBufferOutputStream(buffer);
      }

      @Test
      public void shouldCallFlushStream() throws IOException {
         stream.flush();
         verify(buffer).skip(anyInt());
      }
      
      @Test(expected=IOException.class)
      public void shouldThrowExceptionIfStreamIsClosed() throws IOException {
         stream.close();
         stream.flush();
      }
   }

}
