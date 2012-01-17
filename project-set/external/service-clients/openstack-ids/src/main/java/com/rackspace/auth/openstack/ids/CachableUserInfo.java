package com.rackspace.auth.openstack.ids;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

import java.io.Serializable;
import java.util.Calendar;
import org.openstack.docs.identity.api.v2.Token;

/**
 * @author fran
 */
public class CachableUserInfo implements Serializable {

   private final String tokenId;
   private final String userId;
   private final String username;
   private final String roles;
   private final long expires;

   public CachableUserInfo(AuthenticateResponse response) {
      if (response == null) {
         throw new IllegalArgumentException();
      }
      
      this.tokenId = response.getToken().getId();
      this.userId = response.getUser().getId();
      this.username = response.getUser().getName();
      this.roles = formatRoles(response);
      this.expires = extractExpires(response.getToken());

   }

   private long extractExpires(Token token) {
      long result = 0;
      if (token != null && token.getExpires() != null) {
         result = token.getExpires().toGregorianCalendar().getTimeInMillis();
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

   private String formatRoles(AuthenticateResponse response) {
      String formattedRoles = null;

      if (response.getUser() != null && response.getUser().getRoles() != null) {
         StringBuilder result = new StringBuilder();
         for (Role role : response.getUser().getRoles().getRole()) {
            result.append(role.getName());
            result.append(",");
         }

         formattedRoles = result.substring(0, result.length() - 1);
      }

      return formattedRoles;
   }

   public String getTokenId() {
      return tokenId;
   }

   public String getUserId() {
      return userId;
   }

   public String getUsername() {
      return username;
   }

   public String getRoles() {
      return roles;
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
