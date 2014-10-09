package org.openrepose.commons.utils.http.media.servlet;

import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.http.media.VariantParser;

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
