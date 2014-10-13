package org.openrepose.mocks.auth.provider;

import org.openrepose.mocks.DataProvider;

public interface UserDataProvider extends DataProvider {

   int getUserId(String userName);

   String getUserName(Integer id);

   String[] getValidUsers();
   
   boolean validateUser(String userName);
}
