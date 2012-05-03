package org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps;

import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openstack.docs.identity.api.v2.CredentialType;

import java.util.HashMap;
import java.util.Map;

public class CredentialMap extends HashMap<String, Object> {

   private String getCredentialsType() {
      if (keySet().isEmpty() || keySet().size() > 1) {
         throw new RuntimeException("Invalid auth map");
      }

      return (String) keySet().iterator().next();
   }

   private Map<String, Object> getCredentialsMap(String key) {
      return (Map<String, Object>) get(key);
   }

   public AuthCredentials getCredentials() {
      //String type = getCredentialsType();

      return null;
      // TODO
//      return CredentialFactory.getCredentials(CredentialType.getCredentialType(type), getCredentialsMap(type));
   }
}
