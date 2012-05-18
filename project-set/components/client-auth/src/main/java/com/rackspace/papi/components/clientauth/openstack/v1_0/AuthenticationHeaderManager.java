package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.IdentityStatus;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author fran
 */
public class AuthenticationHeaderManager {
   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHeaderManager.class);

   // Proxy is specified in the OpenStack auth blue print:
   // http://wiki.openstack.org/openstack-authn
   private static final String X_AUTH_PROXY = "Proxy";

   private final String authToken;
   private final AuthToken cachableToken;
   private final Boolean isDelegatable;
   private final FilterDirector filterDirector;
   private final String tenantId;
   private final Boolean validToken;
   private final List<AuthGroup> groups;

   // Hard code QUALITY for now as the auth component will have
   // the highest QUALITY in terms of using the user it supplies for rate limiting
   private static final String QUALITY = ";q=1.0";

   public AuthenticationHeaderManager(String authToken, AuthToken token, Boolean isDelegatable, FilterDirector filterDirector, String tenantId, List<AuthGroup> groups) {
      this.authToken = authToken;
      this.cachableToken = token;
      this.isDelegatable = isDelegatable;
      this.filterDirector = filterDirector;
      this.tenantId = tenantId;
      this.validToken = token != null && token.getTokenId() != null;
      this.groups = groups;
   }

   public void setFilterDirectorValues() {

      if (validToken) {
         filterDirector.setFilterAction(FilterAction.PASS);
         setExtendedAuthorization();
         setUser();
         setRoles();
         setGroups();
         setTenant();

         if (isDelegatable) {
            setIdentityStatus();
         }
      } else if (isDelegatable && nullCredentials()) {
         filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
         setExtendedAuthorization();
         setIdentityStatus();
      }
   }

   private boolean nullCredentials() {
      final boolean nullCreds = StringUtilities.isBlank(authToken) || StringUtilities.isBlank(tenantId);

      LOG.debug("Credentials null = " + nullCreds);
      return nullCreds;
   }

   /**
    * EXTENDED AUTHORIZATION
    */
   private void setExtendedAuthorization() {
      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString(), StringUtilities.isBlank(tenantId) ? X_AUTH_PROXY : X_AUTH_PROXY + " " + tenantId);
   }

   /**
    * IDENTITY STATUS
    */
   private void setIdentityStatus() {
      IdentityStatus identityStatus = IdentityStatus.Confirmed;

      if (!validToken) {
         identityStatus = IdentityStatus.Indeterminate;
      }

      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString(), identityStatus.name());
   }

   /**
    * TENANT
    */
   private void setTenant() {
      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_NAME.toString(), tenantId);
      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), tenantId);
   }

   /**
    * USER
    * The PowerApiHeader is used for Rate Limiting
    * The OpenStackServiceHeader is used for an OpenStack service
    */
   private void setUser() {
      filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.USER.toString(), cachableToken.getUsername() + QUALITY);

      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_NAME.toString(), cachableToken.getUsername());
      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_ID.toString(), cachableToken.getUserId());
   }

   /**
    * ROLES
    * The OpenStackServiceHeader is used for an OpenStack service
    */
   private void setRoles() {
      String roles = cachableToken.getRoles();

      if (StringUtilities.isNotBlank(roles)) {
         filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.ROLES.toString(), roles);
      }
   }

   /**
    * GROUPS
    * The PowerApiHeader is used for Rate Limiting
    */
   private void setGroups() {
      for (AuthGroup group : groups) {
         filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.GROUPS.toString(), group.getId() + QUALITY);
      }
   }

}