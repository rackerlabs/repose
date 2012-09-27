package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class RawInputStreamReaderTest {

   public static class WhenReadingStreams {
      private String data = "Some String of Data";
      private ByteArrayInputStream inStream;
      private RawInputStreamReader reader;

      @Before
      public void setUp() {
         
         reader = RawInputStreamReader.instance();
         inStream = new ByteArrayInputStream(data.getBytes());
      }
      
      @Test
      public void shouldReadBuffer() throws IOException {
         byte[] actual = reader.readFully(inStream);
         
         assertEquals(data, new String(actual));
      }
      
      @Test
      public void shouldReadBufferWithLimit() throws IOException {
         byte[] actual = reader.readFully(inStream, data.length());
         
         assertEquals(data, new String(actual));
      }
      
      @Test(expected=BufferCapacityException.class)
      public void shouldThrowBufferCapacityException() throws IOException {
         byte[] actual = reader.readFully(inStream, 1);
      }
      
      @Test
      public void shouldCopyInputStreamToOutputStream() throws IOException {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         reader.copyTo(inStream, baos);
         
         assertEquals(data, new String(baos.toByteArray()));
      }

      @Test
      public void shouldReturnNumberOfBytesCopied() throws IOException {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         long actual = reader.copyTo(inStream, baos);
         
         assertEquals(data.length(), actual);
      }
   }

}
