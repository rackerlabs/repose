package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.http.header.CustomHeaderParser;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class MediaRangeParser implements CustomHeaderParser<MediaType> {

   private final List<HeaderValue> headerValues;

   public MediaRangeParser(List<HeaderValue> headerValues) {
      this.headerValues = headerValues;
   }

   public MediaRangeParser(Enumeration<String> headerValueEnumeration) {
      this(new HeaderFieldParser(headerValueEnumeration).parse());
   }

   public MediaRangeParser(String rawHeaderString) {
      this(new HeaderFieldParser(rawHeaderString).parse());
   }

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
   public List<MediaType> parse() {
      final List<MediaType> mediaRanges = new LinkedList<MediaType>();
      
      for(HeaderValue headerValue : headerValues) {
         mediaRanges.add(process(headerValue));
      }
      
      return mediaRanges;
   }

   public MediaType process(HeaderValue headerValue) {
      MimeType mediaType = MimeType.getMatchingMimeType(headerValue.getValue());

      if (MimeType.UNKNOWN.equals(mediaType)) {
         mediaType = MimeType.guessMediaTypeFromString(headerValue.getValue());
      }
      
      return new MediaType(headerValue.getValue(), mediaType, headerValue.getParameters());
   }
}
