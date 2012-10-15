package com.rackspace.papi.mocks.auth.osids.providers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.mocks.auth.provider.UserDataProvider;
import org.openstack.docs.identity.api.v2.*;

public interface KeystoneProvider extends UserDataProvider {

   ItemNotFoundFault createItemNotFound();

   TenantForAuthenticateResponse createTenant(String userName);

   Token createToken(String tokenId);

   Token createToken(CredentialType credentialType);

   UnauthorizedFault createUnauthorized(String message);
   
   Group getGroup(String userName, String groupId);

   Groups getGroups(String userName);

   Role getRole(String userName, String roleId);

   RoleList getRoles(String userName);

   UserForAuthenticateResponse getUser(String userName);

   ServiceCatalog getServiceCatalog(String userName);
   
   String getUsernameFromToken(String token);

   boolean isValidToken(String token);
   
   AuthenticateResponse newAuthenticateResponse();
   
   EndpointList getEndpoints(String token);

}
