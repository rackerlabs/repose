package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.MessageDigest;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class MessageDigesterOutputStreamTest {

   public static class WhenUsingDigestStream {
      private MessageDigesterOutputStream stream;
      private MessageDigest digest;
      private byte[] digestBytes = {0, 1, 2, 3, 4, 5};

      @Before
      public void setUp() {
         digest = mock(MessageDigest.class);
         when(digest.digest()).thenReturn(digestBytes);
         stream = new MessageDigesterOutputStream(digest);
      }
      
      @Test
      public void shouldWriteBytes() throws IOException {
         int b = 1;
         stream.write(b);
         verify(digest).update(eq((byte)b));
      }
      
      @Test
      public void shouldGetDigestWhenClosingStream() throws IOException {
         stream.closeStream();
         verify(digest).digest();
      }
      
      @Test
      public void shouldResetDigestWhenFlushingStream() throws IOException {
         stream.flushStream();
         verify(digest).reset();
      }
      
      @Test
      public void shouldGetDigestBytes() throws IOException {
         stream.closeStream();
         byte[] actual = stream.getDigest();
         //assertEquals(digestBytes, actual);
         assertArrayEquals(digestBytes, actual);
      }
      
   }
}
