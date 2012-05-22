package com.rackspace.papi.commons.util.http.media;

import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;

import java.util.Map;

public class MediaType extends HeaderValueImpl {
   private final MimeType mimeType;

   public MediaType(MimeType mimeType) {
      this(mimeType.getMimeType(), mimeType, HeaderValueImpl.DEFAULT_QUALITY);
   }

   public MediaType(MimeType mimeType, double quality) {
      this(mimeType.getMimeType(), mimeType, quality);
   }

   public MediaType(String value, MimeType mimeType) {
       this(value, mimeType, HeaderValueImpl.DEFAULT_QUALITY);
   }
   
   public MediaType(String value, MimeType mimeType, double quality) {
      super(value, quality); //Collections.EMPTY_MAP);

      this.mimeType = mimeType;
   }

   public MediaType(String value, MimeType mediaType, Map<String, String> parameters) {
      super(value, parameters);

      this.mimeType = mediaType;
   }

   public MimeType getMimeType() {
      return mimeType;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      
      if (getClass() != obj.getClass()) {
         return false;
      }
      
      final MediaType other = (MediaType) obj;
      
      if (this.mimeType != other.mimeType) {
         return false;
      }
      
      return super.equals(obj);
   }

   @Override
   public int hashCode() {
      int hash = 3;
      hash = 79 * hash + (this.mimeType != null ? this.mimeType.hashCode() : 0);
      
      return hash + super.hashCode();
   }
}
