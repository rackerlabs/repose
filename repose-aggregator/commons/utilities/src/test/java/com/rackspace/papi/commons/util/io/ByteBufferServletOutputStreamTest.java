package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ByteBufferServletOutputStreamTest {

   public static class WhenWritingData {
      private ByteBuffer buffer;
      private ByteBufferServletOutputStream stream;

      @Before
      public void setUp() {
         buffer = mock(ByteBuffer.class);
         
         stream = new ByteBufferServletOutputStream(buffer);
      }
      
      @Test
      public void shouldWriteByte() throws IOException {
         int b = 1;
         stream.write(b);
         verify(buffer).put(eq((byte)b));
      }

      @Test
      public void shouldWriteBytes() throws IOException {
         byte[] bytes = new byte[10];
         stream.write(bytes);
         verify(buffer).put(eq(bytes));
      }

      @Test
      public void shouldWriteBytesWithOffsetAndLength() throws IOException {
         byte[] bytes = new byte[10];
         int offset = 1;
         int length = 10;
         stream.write(bytes, offset, length);
         verify(buffer).put(eq(bytes), eq(offset), eq(length));
      }
   }

}
