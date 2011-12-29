package com.rackspace.papi.commons.util.http.media.servlet;

import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.http.media.VariantParser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;

import java.util.LinkedList;
import java.util.List;

public abstract class RequestMediaRangeInterrogator {

   private RequestMediaRangeInterrogator() {
   }

   public static List<MediaType> interrogate(String requestUri, String acceptHeader) {
      final List<MediaType> ranges = new LinkedList<MediaType>();

      final MimeType mediaType = VariantParser.getMediaTypeFromVariant(requestUri);

      if (mediaType == null) {
         ranges.addAll(new MediaRangeParser(acceptHeader).parse());
      } else {
         ranges.add(new MediaType(mediaType.getMimeType(), mediaType));
      }

      if (ranges.isEmpty()) {
         ranges.add(new MediaType(MimeType.UNSPECIFIED.getMimeType(), MimeType.UNSPECIFIED));
      }

      return ranges;
   }
}
