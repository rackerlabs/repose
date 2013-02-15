package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.StringUtilities;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

import java.io.Serializable;
import javax.xml.bind.JAXBElement;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fran
 */
public class OpenStackToken extends AuthToken implements Serializable {

   private final String tenantId;
   private final String tenantName;
   private final long expires;
   private final String roles;
   private final String tokenId;
   private final String userId;
   private final String username;
   private final String impersonatorTenantId;
   private final String impersonatorUsername;
   private static final Logger LOG = LoggerFactory.getLogger(OpenStackToken.class);

   public OpenStackToken(AuthenticateResponse response) {
      
      checkTokenInfo(response);

      this.tenantId = response.getToken().getTenant().getId();
      this.tenantName = response.getToken().getTenant().getName();
      this.expires = response.getToken().getExpires().toGregorianCalendar().getTimeInMillis();
      this.roles = formatRoles(response);
      this.tokenId = response.getToken().getId();
      this.userId = response.getUser().getId();
      this.username = response.getUser().getName();
      UserForAuthenticateResponse impersonator = getImpersonator(response);
      if (impersonator != null) {
         this.impersonatorTenantId = impersonator.getId();
         this.impersonatorUsername = impersonator.getName();
      } else {
         this.impersonatorTenantId = "";
         this.impersonatorUsername = "";
      }
   }

   private void checkTokenInfo(AuthenticateResponse response) {
      if (response == null || response.getToken() == null || response.getToken().getExpires() == null) {
         throw new IllegalArgumentException("Invalid token");
      }

      if (response.getToken().getTenant() == null) {
         throw new IllegalArgumentException("Invalid Response from Auth. Token object must have a tenant");
      }

      if (response.getUser() == null) {
         throw new IllegalArgumentException("Invalid Response from Auth: Response must have a user object");
      }

      if (response.getUser().getRoles() == null) {
         throw new IllegalArgumentException("Invalid Response from Auth: User must have a list of roles");
      }
      
   }

   @Override
   public String getTenantId() {
      return tenantId;
   }

   @Override
   public String getTenantName() {
      return this.tenantName;
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

   @Override
   public String getImpersonatorTenantId() {
      return impersonatorTenantId;
   }

   @Override
   public String getImpersonatorUsername() {
      return impersonatorUsername;
   }

   private UserForAuthenticateResponse getImpersonator(AuthenticateResponse response) {
      if (response.getAny() == null) {
         return null;
      }

      for (Object any : response.getAny()) {
         if (any instanceof JAXBElement) {
            JAXBElement element = (JAXBElement) any;
            if (element.getValue() instanceof UserForAuthenticateResponse) {
               return (UserForAuthenticateResponse) element.getValue();
            }
         }
      }

      return null;
   }

   private String formatRoles(AuthenticateResponse response) {
      String formattedRoles = null;

      if (response.getUser() != null && response.getUser().getRoles() != null) {
         StringBuilder result = new StringBuilder();
         for (Role role : response.getUser().getRoles().getRole()) {
            result.append(role.getName());
            result.append(",");
         }

         if (!StringUtilities.isBlank(result.toString())) {
            formattedRoles = result.substring(0, result.length() - 1);
         }else{
            LOG.warn("User with userId " + response.getUser().getId() + " is not associated with any role");
         }
      }

      return formattedRoles;
   }
}
