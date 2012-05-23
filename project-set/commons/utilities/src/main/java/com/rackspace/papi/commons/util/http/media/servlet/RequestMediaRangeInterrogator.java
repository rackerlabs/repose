package com.rackspace.papi.commons.util.http.media.servlet;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.http.media.VariantParser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class RequestMediaRangeInterrogator {

   private RequestMediaRangeInterrogator() {
   }

   public static List<MediaType> interrogate(String requestUri, HeaderValue acceptHeader) {
      List<HeaderValue> accepts = new ArrayList<HeaderValue>();
      accepts.add(acceptHeader);
      return interrogate(requestUri, accepts);
   }
   
   public static List<MediaType> interrogate(String requestUri, List<? extends HeaderValue> acceptHeader) {
      final List<MediaType> ranges = new LinkedList<MediaType>();

      final MimeType mediaType = VariantParser.getMediaTypeFromVariant(requestUri);

      if (mediaType == null) {
         ranges.addAll(new MediaRangeProcessor(acceptHeader).process());
      } else {
         ranges.add(new MediaType(mediaType.getMimeType(), mediaType, 1));
      }

      if (ranges.isEmpty()) {
         ranges.add(new MediaType(MimeType.UNSPECIFIED.getMimeType(), MimeType.UNSPECIFIED, -1));
      }

      return ranges;
   }
}
