package com.rackspace.papi.components.identity.content.maps;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.ObjectFactory;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import java.util.Map;

public class PasswordCredentialsMap {

   private final ObjectFactory factory;
   private final Map<String, Object> map;

   public PasswordCredentialsMap(Map<String, Object> map) {
      this.map = map;
      factory = new ObjectFactory();
   }

   public Credentials getCredentials() {
      PasswordCredentials credentials = factory.createPasswordCredentials();
      credentials.setUsername((String) map.get("username"));
      credentials.setPassword((String) map.get("password"));
      return credentials;
   }
}
