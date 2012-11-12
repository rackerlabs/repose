package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.StringUtilities;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public enum MimeType {

   APPLICATION_XML("application", "xml"),
   APPLICATION_JSON("application", "json"),
   APPLICATION_ATOM_XML("application", "atom+xml"),
   APPLICATION_XHTML_XML("application", "xhtml+xml"),
   APPLICATION_RDF_XML("application", "rdf+xml"),
   APPLICATION_RSS_XML("application", "rss+xml"),
   APPLICATION_SOAP_XML("application", "soap+xml"),
   APPLICATION_XML_DTD("application", "xml-dtd"),
   APPLICATION_XOP_XML("application", "xop+xml"),
   TEXT_HTML("text", "html"),
   TEXT_PLAIN("text", "plain"),
   TEXT_XML("text","xml"),
   IMAGE_SVG_XML("image","svg+xml"),
   MESSAGE_IMDN_XML("message","imdn+xml"),
   MODEL_X3D_XML("model","x3d+xml"),
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
         
         for(MimeType ct : values()){
            if(mimeType.contains(ct.getType()) && mimeType.contains(ct.getSubType())){
               return ct;
            }
         }
         
         
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
