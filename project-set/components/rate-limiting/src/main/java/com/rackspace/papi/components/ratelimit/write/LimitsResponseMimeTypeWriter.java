package com.rackspace.papi.components.ratelimit.write;

import com.rackspace.repose.service.ratelimit.config.LimitsFormat;
import com.rackspace.papi.components.ratelimit.util.LimitsEntityStreamTransformer;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LimitsResponseMimeTypeWriter {

   private final LimitsEntityStreamTransformer responseTransformer;
   private final LimitsFormat limitsFormat;

   public LimitsResponseMimeTypeWriter(LimitsFormat limitsFormat, LimitsEntityStreamTransformer responseTransformer) {
      this.limitsFormat = limitsFormat;
      this.responseTransformer = responseTransformer;
   }

   public void writeLimitsResponse(byte[] readableContents, MediaType mediaType, OutputStream outputStream) throws IOException {
      if (MediaType.APPLICATION_XML.equals(mediaType)) {
         outputStream.write(readableContents);
      } else {
         // default to json for now
         responseTransformer.streamAsJson(new ByteArrayInputStream(readableContents), outputStream, limitsFormat);
      }     
   }
}
