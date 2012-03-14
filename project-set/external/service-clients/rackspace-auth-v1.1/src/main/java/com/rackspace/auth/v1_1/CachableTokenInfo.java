package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import java.io.Serializable;
import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachableTokenInfo implements Serializable {
   private static final Logger LOG = LoggerFactory.getLogger(CachableTokenInfo.class);
   private final String tenantId;
   private final String userId;
   private final String tokenId;
   private final long expires;
   
   public CachableTokenInfo(String tenantId, FullToken token) {
      if (token == null) {
         throw new IllegalArgumentException("Invalid token");
      }
      
      this.tenantId = tenantId;
      this.userId = token.getUserId();
      this.tokenId = token.getId();
      this.expires = extractExpires(token);
   }

   private long extractExpires(FullToken token) {
      long result = 0;
      if (token != null && token.getExpires() != null) {
         result = token.getExpires().toGregorianCalendar().getTimeInMillis();
      } else {
         LOG.warn("Unable to determine token expiration for tenant: " + tenantId);
      }
      
      return result;
   }
   
   private Long determineTtlInMillis() {
      long ttl = 0;

      if (expires > 0) {
         ttl = expires - Calendar.getInstance().getTimeInMillis();
      }

      return ttl > 0 ? ttl : 0;
   }

   public String getTenantId() {
      return tenantId;
   }
   
   public String getUserId() {
      return userId;
   }
   
   public String getTokenId() {
      return tokenId;
   }
   
   public long getExpires() {
      return expires;
   }
   
   public Long tokenTtl() {
      return determineTtlInMillis();
   }
   
   public int safeTokenTtl() {
      Long tokenTtl = tokenTtl();
      
      if (tokenTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }
      
      return tokenTtl.intValue();
   }
}
