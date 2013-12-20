package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Enclosed.class)
public class InputStreamMergerTest {

   public static class WhenClosingMergedStreams {
      private InputStream stream1;
      private InputStream stream2;
      private InputStream stream3;
      private InputStream merged;
      private InputStream mergedWithNull;
      
      @Before
      public void setup() {
         stream1 = mock(InputStream.class);
         stream2 = mock(InputStream.class);
         stream3 = mock(InputStream.class);
         merged = InputStreamMerger.merge(stream1, stream2, stream3);
         mergedWithNull = InputStreamMerger.merge(stream1, null, stream2, null, stream3, null);
      }
      
      @Test
      public void shouldCloseAllStreams() throws IOException {
         merged.close();
         
         verify(stream1).close();
         verify(stream2).close();
         verify(stream3).close();
      }

      @Test
      public void shouldSkipNullStreams() throws IOException {
         mergedWithNull.close();
         
         verify(stream1).close();
         verify(stream2).close();
         verify(stream3).close();
      }
   }
   
   public static class WhenMarkingStreams {
      private InputStream stream;
      @Before
      public void setup() {
         stream = InputStreamMerger.merge();
         
      }
      @Test
      public void shouldNotSupportMarking() {
         assertFalse(stream.markSupported());
      }
      
      @Test
      public void shouldIgnoreMarkRequests() {
         stream.mark(1);
      }
   }
   
   public static class WhenSkippingMergedStreams {
      private final String STREAM_DATA1 = "Stream1";
      private final String STREAM_DATA2 = "Stream2";
      private final String STREAM_DATA3 = "Stream3";
      private InputStream stream1;
      private InputStream stream2;
      private InputStream stream3;
      private InputStreamMerger combinedStream;

      @Before
      public void setUp() {
         stream1 = InputStreamMerger.wrap(STREAM_DATA1);
         stream2 = InputStreamMerger.wrap(STREAM_DATA2);
         stream3 = InputStreamMerger.wrap(STREAM_DATA3);
         combinedStream = (InputStreamMerger) InputStreamMerger.merge(stream1, stream2, stream3);
      }

      @Test
      public void shouldSkipWithinFirstStream() throws IOException {
         long expected = STREAM_DATA1.length() - 1;
         long actual = combinedStream.skip(expected);
      
         assertEquals(expected, actual);
      }
      
      @Test
      public void shouldSkipMultipleStreamsOfData() throws IOException {
         long toSkip = (STREAM_DATA1 + STREAM_DATA2).length();
         long skipped = combinedStream.skip(toSkip);

         assertEquals(toSkip, skipped);

         BufferedReader reader = new BufferedReader(new InputStreamReader(combinedStream));
         String result = reader.readLine();

         assertEquals(STREAM_DATA3, result);
      }

      @Test
      public void shouldHandleSkippingBeyondEndOfData() throws IOException {
         long expected = (STREAM_DATA1 + STREAM_DATA2 + STREAM_DATA3).length();
         long toSkip = expected + 10;
         long actual = combinedStream.skip(toSkip);

         assertEquals(expected, actual);
      }

   }
   
   public static class WhenReadingMergedStreams {

      private final String STREAM_DATA1 = "Stream1";
      private final String STREAM_DATA2 = "Stream2";
      private final String STREAM_DATA3 = "Stream3";
      private InputStream stream1;
      private InputStream stream2;
      private InputStream stream3;
      private InputStreamMerger combinedStream;

      @Before
      public void setUp() {
         stream1 = InputStreamMerger.wrap(STREAM_DATA1);
         stream2 = InputStreamMerger.wrap(STREAM_DATA2);
         stream3 = InputStreamMerger.wrap(STREAM_DATA3);
         combinedStream = (InputStreamMerger) InputStreamMerger.merge(stream1, stream2, stream3);
      }

      @Test
      public void shouldWrapAStringAsAStream() throws IOException {
         BufferedReader reader = new BufferedReader(new InputStreamReader(stream1));
         String result = reader.readLine();

         assertEquals(STREAM_DATA1, result);
      }

      @Test
      public void shouldMergeMultipleInputStreams() throws IOException {
         BufferedReader reader = new BufferedReader(new InputStreamReader(combinedStream));
         String result = reader.readLine();

         assertEquals(STREAM_DATA1 + STREAM_DATA2 + STREAM_DATA3, result);
      }

      @Test
      public void shouldReturnAvailableOfCurrentStream() throws IOException {
         int available = combinedStream.available();

         assertEquals(STREAM_DATA1.length(), available);
      }

      @Test
      public void shouldReadEntireStream() throws IOException {
         long expected = (STREAM_DATA1 + STREAM_DATA2 + STREAM_DATA3).length();
         int value = combinedStream.read();
         int actualReadCount = 0;

         while (value >= 0) {
            actualReadCount++;
            value = combinedStream.read();
         }

         assertEquals(expected, actualReadCount);

      }

      @Test(expected = IOException.class)
      public void shouldNotSupportReset() throws IOException {
         combinedStream.reset();
      }

      @Test
      public void shouldReadIntoByteArray() throws IOException {
         byte[] bytes = new byte[(STREAM_DATA1 + STREAM_DATA2).length()];
         String expected = STREAM_DATA1 + STREAM_DATA2;
         int readLength = combinedStream.read(bytes);

         assertEquals(bytes.length, readLength);

         String actual = new String(bytes);
         System.out.println(expected);
         assertEquals(expected, actual);
      }

      @Test
      public void shouldHandleEmptyStreams() throws IOException {
         InputStream emptyStream1 = InputStreamMerger.wrap("");
         InputStream emptyStream2 = InputStreamMerger.wrap("");
         InputStream emptyStream3 = InputStreamMerger.wrap("");
         InputStream emptyStream = InputStreamMerger.merge(emptyStream1, emptyStream2, emptyStream3);

         int expected = -1;

         assertEquals(expected, emptyStream.read());
      }

      @Test
      public void shouldHandleNullStreams1() throws IOException {
         InputStream stream = InputStreamMerger.merge(null, stream2, stream3);
         String expected = STREAM_DATA2 + STREAM_DATA3;


         BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
         String result = reader.readLine();

         assertEquals(expected, result);
      }

      @Test
      public void shouldHandleNullStreams2() throws IOException {
         InputStream stream = InputStreamMerger.merge(stream1, null, stream3);
         String expected = STREAM_DATA1 + STREAM_DATA3;


         BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
         String result = reader.readLine();

         assertEquals(expected, result);
      }

      @Test
      public void shouldHandleNullStreams3() throws IOException {
         InputStream stream = InputStreamMerger.merge(stream1, stream2, null);
         String expected = STREAM_DATA1 + STREAM_DATA2;


         BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
         String result = reader.readLine();

         assertEquals(expected, result);
      }

      @Test
      public void shouldHandleNullStreams4() throws IOException {
         InputStream stream = InputStreamMerger.merge((InputStream)null);
         int expected = -1;
         
         assertEquals(expected, stream.read());
      }

      @Test
      public void shouldHandleNullStreams5() throws IOException {
         InputStream stream = InputStreamMerger.merge();
         int expected = -1;
         
         assertEquals(expected, stream.read());
      }
   }
}
