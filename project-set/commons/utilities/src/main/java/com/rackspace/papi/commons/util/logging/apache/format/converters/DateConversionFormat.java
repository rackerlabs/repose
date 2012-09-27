package com.rackspace.papi.commons.util.logging.apache.format.converters;

public enum DateConversionFormat {

   RFC_1123("E, dd MMM yyyy HH:mm:ss z"),
   ISO_8601("yyyy-MM-dd'T'HH:mm:ssz");
   private String pattern;

   DateConversionFormat(String pattern) {
      this.pattern = pattern;
   }

   public static String getPattern(String name) {
      for (DateConversionFormat format : DateConversionFormat.values()) {
         if (format.name().equals(name)) {
            return format.pattern;
         }
      }

      return RFC_1123.pattern;
   }

   public String getPattern() {
      return this.pattern;
   }
}
