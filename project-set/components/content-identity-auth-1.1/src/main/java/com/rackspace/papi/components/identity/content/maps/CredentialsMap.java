package com.rackspace.papi.components.identity.content.maps;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import java.util.HashMap;
import java.util.Map;

public class CredentialsMap extends HashMap<String, Object> {
   
   public CredentialsMap() {
   }
   
   public Credentials getCredentials() {
      if (keySet().isEmpty()) {
         return null;
      }
      
      String type = keySet().iterator().next();
      Map<String, Object> map = (Map<String, Object>)get(type);
      
      if ("passwordCredentials".equals(type)) {
         return new PasswordCredentialsMap(map).getCredentials();
      }
      
      return null;
   }
}
