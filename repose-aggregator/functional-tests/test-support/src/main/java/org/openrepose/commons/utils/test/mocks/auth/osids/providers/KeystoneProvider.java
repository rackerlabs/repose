/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.test.mocks.auth.osids.providers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openrepose.commons.utils.test.mocks.auth.provider.UserDataProvider;
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
