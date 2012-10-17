package com.rackspace.papi.components.ratelimit.write;

import com.rackspace.papi.components.ratelimit.util.LimitsEntityStreamTransformer;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LimitsResponseMimeTypeWriter {

   private final LimitsEntityStreamTransformer responseTransformer;

   public LimitsResponseMimeTypeWriter(LimitsEntityStreamTransformer responseTransformer) {
      this.responseTransformer = responseTransformer;
   }

   public MediaType writeLimitsResponse(byte[] readableContents, MediaType mediaType, OutputStream outputStream) throws IOException {
      if (MediaType.APPLICATION_XML_TYPE.equals(mediaType)) {
         outputStream.write(readableContents);
         return MediaType.APPLICATION_XML_TYPE;
      } else {
         // default to json for now
         responseTransformer.streamAsJson(new ByteArrayInputStream(readableContents), outputStream);
         return MediaType.APPLICATION_JSON_TYPE;
      }     
   }
}
