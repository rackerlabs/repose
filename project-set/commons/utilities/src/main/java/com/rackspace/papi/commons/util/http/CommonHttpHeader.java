package com.rackspace.papi.commons.util.http;

/**
 *
 *
 */
public enum CommonHttpHeader implements HeaderConstant {

   //Auth specific
   AUTH_TOKEN("X-Auth-Token"),
   AUTHORIZATION("Authorization"),
   WWW_AUTHENTICATE("WWW-Authenticate"),
   
   //Standards
   HOST("Host"),
   RETRY_AFTER("Retry-After"),
   EXPIRES("Expires"),
   X_FORWARDED_FOR("X-Forwarded-For"),
   USER_AGENT("User-Agent"),
   VIA("Via"),
   LOCATION("Location"),
   
   //Content specific
   ACCEPT("Accept"),
   CONTENT_TYPE("Content-Type"),
   CONTENT_LENGTH("Content-Length");
   
   private final String headerKey;

   private CommonHttpHeader(String headerKey) {
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