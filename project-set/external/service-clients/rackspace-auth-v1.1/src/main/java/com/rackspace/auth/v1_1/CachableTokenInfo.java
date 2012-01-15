package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.xml.datatype.XMLGregorianCalendar;

public class CachableTokenInfo implements Serializable {
   private final String userId;
   private final String tokenId;
   private final Long tokenTtl;
   
   public CachableTokenInfo(FullToken token) {
      if (token == null) {
         throw new IllegalArgumentException("Invalid token");
      }
      this.userId = token.getUserId();
      this.tokenId = token.getId();
      this.tokenTtl = determineTtlInMillis(token);
   }
   
   private Long determineTtlInMillis(FullToken token) {
      long ttl = 0;

      if (token != null && token.getExpires() != null) {
         Calendar current = Calendar.getInstance();
         Calendar expires = token.getExpires().toGregorianCalendar();
         
         //if (expires.getTimeZone().equals(current.getTimeZone())) {
            ttl = token.getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
         //}
      }

      return ttl > 0 ? ttl : 0;
   }

   public String getUserId() {
      return userId;
   }
   
   public String getTokenId() {
      return tokenId;
   }
   
   public Long getTokenTtl() {
      return tokenTtl;
   }
   
   public int getSafeTokenTtl() {
      if (tokenTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }
      
      return tokenTtl.intValue();
   }
}
