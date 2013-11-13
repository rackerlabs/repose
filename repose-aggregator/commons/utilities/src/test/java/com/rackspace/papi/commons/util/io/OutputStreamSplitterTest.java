package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class OutputStreamSplitterTest {

   public static class WhenSplittingOutputStreams {
      private OutputStream stream1;
      private OutputStream stream2;
      private OutputStream stream3;
      private OutputStreamSplitter splitter;

      @Before
      public void setUp() {
         stream1 = mock(OutputStream.class);
         stream2 = mock(OutputStream.class);
         stream3 = mock(OutputStream.class);
         
         splitter = new OutputStreamSplitter(stream1, stream2, stream3);
      }
      
      @Test
      public void shouldWriteToAllStreams() throws IOException {
         int value = 1;
         splitter.write(value);
         
         verify(stream1).write(eq(value));
         verify(stream2).write(eq(value));
         verify(stream3).write(eq(value));
      }

      @Test
      public void shouldWriteBytesToAllStreams() throws IOException {
         byte[] value = {1, 2, 3, 4, 5};
         splitter.write(value);
         
         verify(stream1).write(eq(value));
         verify(stream2).write(eq(value));
         verify(stream3).write(eq(value));
      }

      @Test
      public void shouldWriteBytesToAllStreams2() throws IOException {
         byte[] value = {1, 2, 3, 4, 5};
         int i = 3;
         int i1 = 7;
         
         splitter.write(value, i, i1);
         
         verify(stream1).write(eq(value), eq(i), eq(i1));
         verify(stream2).write(eq(value), eq(i), eq(i1));
         verify(stream3).write(eq(value), eq(i), eq(i1));
      }

      @Test
      public void shouldCloseAllStreams() throws IOException {
         splitter.close();
         
         verify(stream1).close();
         verify(stream2).close();
         verify(stream3).close();
      }

      @Test
      public void shouldFlushAllStreams() throws IOException {
         splitter.flush();
         
         verify(stream1).flush();
         verify(stream2).flush();
         verify(stream3).flush();
      }

   }

}
