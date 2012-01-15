package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import java.io.Serializable;
import java.util.Calendar;

public class CachableTokenInfo implements Serializable {
   private final String userId;
   private final String tokenId;
   private final Calendar expires;
   
   public CachableTokenInfo(FullToken token) {
      if (token == null) {
         throw new IllegalArgumentException("Invalid token");
      }
      this.userId = token.getUserId();
      this.tokenId = token.getId();
      this.expires = getExpires(token);
   }
   private Calendar getExpires(FullToken token) {
      Calendar result = null;
      if (token != null && token.getExpires() != null) {
         result = token.getExpires().toGregorianCalendar();
      }
      
      return result;
   }
   
   private Long determineTtlInMillis() {
      long ttl = 0;

      if (expires != null) {
         ttl = expires.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
      }

      return ttl > 0 ? ttl : 0;
   }

   
   public String getUserId() {
      return userId;
   }
   
   public String getTokenId() {
      return tokenId;
   }
   
   public Calendar getExpires() {
      return expires;
   }
   
   public Long getTokenTtl() {
      return determineTtlInMillis();
   }
   
   public int getSafeTokenTtl() {
      Long tokenTtl = getTokenTtl();
      
      if (tokenTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }
      
      return tokenTtl.intValue();
   }
}
