package com.rackspace.papi.commons.util.http;

/**
 *
 * @author jhopper
 */
public enum PowerApiHeader implements HeaderConstant {

   NEXT_ROUTE("X-PP-Next-Route"),
   RATE_LIMITED("X-PP-Rate-Limited"),
   USER("X-PP-User"),
   GROUPS("X-PP-Groups");
   
   private final String headerKey;

   private PowerApiHeader(String headerKey) {
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
