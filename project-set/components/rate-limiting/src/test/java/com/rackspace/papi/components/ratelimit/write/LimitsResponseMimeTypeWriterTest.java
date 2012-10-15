package com.rackspace.papi.components.ratelimit.write;

import com.rackspace.papi.components.ratelimit.util.LimitsEntityStreamTransformer;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class LimitsResponseMimeTypeWriterTest {

   public static class WhenWriting {
      private final LimitsResponseMimeTypeWriter writer;
      private final byte[] readableContents = {42};
      private final OutputStream out;
      private final LimitsEntityStreamTransformer transformer;

      public WhenWriting() throws IOException {
         transformer = mock(LimitsEntityStreamTransformer.class);
         out = mock(OutputStream.class);
         final InputStream in = mock(InputStream.class);
         this.writer = new LimitsResponseMimeTypeWriter(transformer);

         doNothing().when(transformer).streamAsJson(in, out);
         doNothing().when(out).write(readableContents);
      }

      @Test
      public void shouldChooseXmlPath() throws IOException {
         writer.writeLimitsResponse(readableContents, MediaType.APPLICATION_XML_TYPE, out);

         verify(out, times(1)).write(readableContents);
      }

      @Test
      public void shouldChooseJsonPath() throws IOException {
         writer.writeLimitsResponse(readableContents, MediaType.APPLICATION_JSON_TYPE, out);

         verify(transformer, times(1)).streamAsJson(any(InputStream.class), any(OutputStream.class));
      }
   }

}
