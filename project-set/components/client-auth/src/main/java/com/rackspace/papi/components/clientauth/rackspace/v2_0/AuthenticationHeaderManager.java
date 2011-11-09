package com.rackspace.papi.components.clientauth.rackspace.v2_0;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.rackspace.IdentityStatus;
import com.rackspace.papi.components.clientauth.rackspace.config.User;
import com.rackspace.papi.components.clientauth.rackspace.config.UserRoles;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class AuthenticationHeaderManager {
    private final AuthenticateResponse authenticateResponse;
    private final Boolean isDelegatable;
    private final FilterDirector filterDirector;
    private final String tenantId;

    public AuthenticationHeaderManager(AuthenticateResponse authenticateResponse, Boolean isDelegatable, FilterDirector filterDirector, String tenantId) {
        this.authenticateResponse = authenticateResponse;
        this.isDelegatable = isDelegatable;
        this.filterDirector = filterDirector;
        this.tenantId = tenantId;
    }

    public void setFilterDirectorValues() {
      Boolean validToken = false;
      if (authenticateResponse.getToken().getId() != null) {
        validToken = true;
      }
      filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.EXTENDED_AUTHORIZATION.headerKey(), "proxy " + tenantId);

      if (validToken || isDelegatable) {
         filterDirector.setFilterAction(FilterAction.PASS);
      }

      if (validToken) {
//         filterDirector.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.headerKey(), getGroupsListIds(tenantId));
         filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), tenantId);

         // This is temporary to support roles until we integrate with Auth 2.0
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.TENANT.headerKey(), tenantId);
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.TENANT_ID.headerKey(), tenantId);

//            List<String> roleList = new ArrayList<String>();
//
//            for (String r : userRoles.getDefault().getRoles().getRole()) {
//               roleList.add(r);
//            }
//
//            for (User user : userRoles.getUser()) {
//               if (user.getName().equalsIgnoreCase(accountUsername)) {
//                  for (String r : user.getRoles().getRole()) {
//                     roleList.add(r);
//                  }
//               }
//            }
//
//            if (roleList.size() > 0) {
//               filterDirector.requestHeaderManager().putHeader(PowerApiHeader.ROLES.headerKey(), roleList.toArray(new String[0]));
//            }

         //  end temporary keystone support
      }

      if (isDelegatable) {
         IdentityStatus identityStatus = IdentityStatus.Confirmed;

         if (!validToken) {
            identityStatus = IdentityStatus.Indeterminate;
         }

         filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.IDENTITY_STATUS.headerKey(), identityStatus.name());
      }


   }
}
