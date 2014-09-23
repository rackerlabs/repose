package com.rackspace.papi.mocks.auth.rs11.providers;

import com.rackspace.papi.mocks.auth.provider.UserDataProvider;
import com.rackspacecloud.docs.auth.api.v1.*;

public interface AuthProvider extends UserDataProvider {

   ItemNotFoundFault createItemNotFound();

   FullToken createToken(String userName, String id);

   UnauthorizedFault createUnauthorized();

   Group getGroup(String userName, String groupId);

   GroupsList getGroups(String userName);
   
   boolean validateToken(String userName, String token, String accountType);
   
   String getUsername(String userId);
}
