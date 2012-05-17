package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthToken;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

import java.io.Serializable;

/**
 * @author fran
 */
public class OpenStackToken extends AuthToken implements Serializable {
   private final String tenantId;
   private final long expires;
   private final String roles;

   private final String tokenId;
   private final String userId;
   private final String username;

   public OpenStackToken(String tenantId, AuthenticateResponse response) {
      if (response == null || response.getToken() == null || response.getToken().getExpires() == null ) {
         throw new IllegalArgumentException("Invalid token");
      }

      this.tenantId = tenantId;
      this.expires = response.getToken().getExpires().toGregorianCalendar().getTimeInMillis();
      this.roles = formatRoles(response);
      this.tokenId = response.getToken().getId();
      this.userId = response.getUser().getId();
      this.username = response.getUser().getName(); 
   }

   @Override
   public String getTenantId() {
      return tenantId;
   }

   @Override
   public String getUserId() {
      return userId;
   }

   @Override
   public String getTokenId() {
      return tokenId;
   }

   @Override
   public long getExpires() {
      return expires;
   }

   @Override
   public String getUsername() {
      return username;
   }

   @Override
   public String getRoles() {
      return roles;
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
}
