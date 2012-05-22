package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.AuthGroup;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.IdentityStatus;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;

import java.util.List;

/**
 * @author fran
 */
public class RackspaceAuthenticationHeaderManager {

   // Proxy is specified in the OpenStack auth blue print:
   // http://wiki.openstack.org/openstack-authn
   private static final String X_AUTH_PROXY = "Proxy";

   private final boolean validToken;
   private final boolean isDelegatable;
   private final FilterDirector filterDirector;
   private final String accountUsername;
   private final List<AuthGroup> groups;

   // Hard code QUALITY for now as the auth component will have
   // the highest QUALITY in terms of using the user it supplies for rate limiting
   private static final String QUALITY = ";q=1";

   public RackspaceAuthenticationHeaderManager(boolean validToken, boolean delegatable, FilterDirector filterDirector, String accountUsername, List<AuthGroup> groups) {
      this.validToken = validToken;
      this.isDelegatable = delegatable;
      this.filterDirector = filterDirector;
      this.accountUsername = accountUsername;
      this.groups = groups;
   }

   public void setFilterDirectorValues() {

      setIdentityStatus();

      if (validToken || isDelegatable) {
         filterDirector.setFilterAction(FilterAction.PASS);
      }

      if (validToken) {
         getGroupsListIds();
         filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.USER.toString(), accountUsername + QUALITY);
      }
   }

   private void getGroupsListIds() {      
      for (AuthGroup group : groups) {
         filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.GROUPS.toString(), group.getId() + QUALITY);
      }
   }

   /**
    * Set Identity Status and X-Authorization headers
    */
   private void setIdentityStatus() {
      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString(), StringUtilities.isBlank(accountUsername) ? X_AUTH_PROXY : X_AUTH_PROXY + " " + accountUsername);

      if (isDelegatable) {
         IdentityStatus identityStatus = IdentityStatus.Confirmed;

         if (!validToken) {
            identityStatus = IdentityStatus.Indeterminate;
         }

         filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString(), identityStatus.name());
      }
   }
}
