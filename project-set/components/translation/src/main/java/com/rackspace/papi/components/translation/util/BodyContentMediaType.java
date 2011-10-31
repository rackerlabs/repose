package com.rackspace.papi.components.translation.util;

import java.util.regex.Pattern;

public enum BodyContentMediaType {

   XML("(?i)[^/]+/(?:[^;]+\\.)*" + BodyContentMediaType.XML_VALUE + "(?:\\.[^;]+)*(?:$|;.*)"), // matches strings like application/xml or application/vendor.xml.somestring;param=1
   JSON("(?i)[^/]+/(?:[^;]+\\.)*" + BodyContentMediaType.JSON_VALUE + "(?:\\.[^;]+)*(?:$|;.*)"), // matches strings like application/json or application/vendor.json.somestring;param=1
   UNKNOWN(".*");
   private static final String JSON_VALUE = "json";
   private static final String XML_VALUE = "xml";
   private final Pattern pattern;

   BodyContentMediaType(String regex) {
      pattern = Pattern.compile(regex);
   }

   Pattern getPattern() {
      return pattern;
   }

   public static BodyContentMediaType getMediaType(String contentType) {
      for (BodyContentMediaType mediaType : values()) {
         if (mediaType.getPattern().matcher(contentType).matches()) {
            return mediaType;
         }
      }

      return UNKNOWN;
   }
}
