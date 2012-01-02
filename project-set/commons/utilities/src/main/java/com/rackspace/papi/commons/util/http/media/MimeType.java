package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.StringUtilities;

/**
 *
 *
 */
public enum MimeType {

   APPLICATION_XML("application", "xml"),
   APPLICATION_JSON("application", "json"),
   APPLICATION_ATOM_XML("application", "atom+xml"),
   APPLICATION_XHTML_XML("application", "xhtml+xml"),
   TEXT_HTML("text", "html"),
   TEXT_PLAIN("text", "plain"),
   WILDCARD("*", "*"),
   UNKNOWN("", ""),
   UNSPECIFIED("", "");

   public static MimeType getMatchingMimeType(String mimeType) {
      if (StringUtilities.isNotBlank(mimeType)) {
         for (MimeType ct : values()) {
            if (ct.getMimeType().equalsIgnoreCase(mimeType)) {
               return ct;
            }
         }
      }
      
      return UNKNOWN;
   }

   public static MimeType guessMediaTypeFromString(String mimeType) {
      if (StringUtilities.isNotBlank(mimeType)) {
         for (MimeType ct : values()) {
            if (mimeType.contains(ct.getSubType())) {
               return ct;
            }
         }
      }

      return UNKNOWN;
   }
   private final String type, subType, mimeType;

   private MimeType(String type, String subType) {
      this.type = type;
      this.subType = subType;

      this.mimeType = type + "/" + subType;
   }

   @Override
   public String toString() {
      return mimeType;
   }

   public String getMimeType() {
      return mimeType;
   }

   public String getSubType() {
      return subType;
   }

   public String getType() {
      return type;
   }
}
