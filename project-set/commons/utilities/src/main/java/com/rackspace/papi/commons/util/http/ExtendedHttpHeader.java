package com.rackspace.papi.commons.util.http;

/**
 *
 * @author zinic
 */
public enum ExtendedHttpHeader implements HeaderConstant {

   //Caching specific
   X_TTL("X-TTL");
   
   private final String headerKey;

   private ExtendedHttpHeader(String headerKey) {
      this.headerKey = headerKey.toLowerCase();
   }

   @Override
   public String toString() {
      return headerKey;
   }

   @Override
   public boolean matches(String st) {
      return headerKey.equalsIgnoreCase(st);
   }
}