package com.rackspace.papi.components.identity.content.credentials.maps;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.wrappers.MossoCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.NastCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.PasswordCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.UserCredentialsWrapper;
import java.util.Map;

public class CredentialFactory {

   public static AuthCredentials getCredentials(CredentialType type, Map<String, Object> credentials) {
      switch(type) {
         case MOSSO:
            return new MossoCredentialsWrapper(credentials);
         case NAST:
            return new NastCredentialsWrapper(credentials);
         case PASSWORD: 
            return new PasswordCredentialsWrapper(credentials);
         case USER: 
            return new UserCredentialsWrapper(credentials);
      }
      
      return null;
   }
}
