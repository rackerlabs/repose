package org.openrepose.commons.utils.test.mocks.auth.provider;

import org.openrepose.commons.utils.test.mocks.DataProvider;

public interface UserDataProvider extends DataProvider {

   int getUserId(String userName);

   String getUserName(Integer id);

   String[] getValidUsers();
   
   boolean validateUser(String userName);
}
