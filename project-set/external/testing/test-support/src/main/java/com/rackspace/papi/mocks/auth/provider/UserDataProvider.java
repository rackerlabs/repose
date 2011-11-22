package com.rackspace.papi.mocks.auth.provider;

import com.rackspace.papi.mocks.DataProvider;

public interface UserDataProvider extends DataProvider {

   int getUserId(String userName);

   String getUserName(Integer id);

   String[] getValidUsers();
   
   boolean validateUser(String userName);
}
