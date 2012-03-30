package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableUserInfo;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.IdentityStatus;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class AuthenticationHeaderManager {

   // Proxy is specified in the OpenStack auth blue print:
   // http://wiki.openstack.org/openstack-authn
   private static final String X_AUTH_PROXY = "Proxy";

   private final String authToken;
   private final CachableUserInfo cachableTokenInfo;
   private final Boolean isDelegatable;
   private final FilterDirector filterDirector;
   private final String tenantId;
   private final Boolean validToken;
   private final Groups groups;
   private final HttpServletRequest request;

   // Hard code QUALITY for now as the auth component will have
   // the highest QUALITY in terms of using the user it supplies for rate limiting
   private final static String QUALITY = ";q=1.0";

    public AuthenticationHeaderManager(String authToken, CachableUserInfo cachableTokenInfo, Boolean isDelegatable, FilterDirector filterDirector, String tenantId, Groups groups, HttpServletRequest request) {
      this.authToken = authToken;
      this.cachableTokenInfo = cachableTokenInfo;
      this.isDelegatable = isDelegatable;
      this.filterDirector = filterDirector;
      this.tenantId = tenantId;
      this.validToken = cachableTokenInfo != null && cachableTokenInfo.getTokenId() != null;
      this.groups = groups;
      this.request = request;
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
      return StringUtilities.isBlank(authToken) || StringUtilities.isBlank(tenantId);
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
      filterDirector.requestHeaderManager().appendToHeader(request, PowerApiHeader.USER.toString(), cachableTokenInfo.getUsername() + QUALITY);

      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_NAME.toString(), cachableTokenInfo.getUsername());
      filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_ID.toString(), cachableTokenInfo.getUserId());
   }

   /**
    * ROLES
    * The OpenStackServiceHeader is used for an OpenStack service
    */
   private void setRoles() {
      String roles = cachableTokenInfo.getRoles();

      if (StringUtilities.isNotBlank(roles)) {
         filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.ROLES.toString(), roles);
      }
   }

   /**
    * GROUPS
    * The PowerApiHeader is used for Rate Limiting
    */
   private void setGroups() {
      if (groups != null) {

         List<String> groupIds = new ArrayList<String>();
         for (Group group : groups.getGroup()) {
            groupIds.add(group.getId());
            filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.GROUPS.toString(), group.getId() + QUALITY);
         }
      }
   }
}