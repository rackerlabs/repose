package org.openrepose.commons.utils.http.media;

import org.openrepose.commons.utils.http.header.HeaderValue;

import java.util.ArrayList;
import java.util.List;

public class MediaRangeProcessor {
   private final List<? extends HeaderValue> values;
   
   public MediaRangeProcessor(List<? extends HeaderValue> values) {
      this.values = values;
   }
   
   public List<MediaType> process() {
      List<MediaType> result = new ArrayList<MediaType>();
      
      for (HeaderValue value: values) {
         result.add(process(value));
      }
      
      return result;
   }

   public MediaType process(HeaderValue headerValue) {
       String mediaTypeWithParamtersStripped = headerValue.getValue().split(";")[0];

      MimeType mediaType = MimeType.getMatchingMimeType(mediaTypeWithParamtersStripped);

      if (MimeType.UNKNOWN.equals(mediaType)) {
         mediaType = MimeType.guessMediaTypeFromString(mediaTypeWithParamtersStripped);
      }
      
      return new MediaType(mediaTypeWithParamtersStripped, mediaType, headerValue.getParameters());
   }
}
