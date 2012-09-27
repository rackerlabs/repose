package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.http.header.HeaderValue;

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
      MimeType mediaType = MimeType.getMatchingMimeType(headerValue.getValue());

      if (MimeType.UNKNOWN.equals(mediaType)) {
         mediaType = MimeType.guessMediaTypeFromString(headerValue.getValue());
      }
      
      return new MediaType(headerValue.getValue(), mediaType, headerValue.getParameters());
   }
}
