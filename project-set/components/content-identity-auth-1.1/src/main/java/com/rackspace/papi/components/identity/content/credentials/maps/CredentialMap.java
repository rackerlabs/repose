package com.rackspace.papi.components.identity.content.credentials.maps;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;

import java.util.HashMap;
import java.util.Map;

public class CredentialMap extends HashMap<String, Object> {
   
   public static class CredentialException extends RuntimeException {
      public CredentialException(String message) {
         super(message);
      }
      
      public CredentialException(Throwable cause) {
         super(cause);
      }
   }

   private String getCredentialsType() {
      if (keySet().isEmpty() || keySet().size() > 1) {
         throw new CredentialException("Invalid auth map");
      }

      return (String) keySet().iterator().next();
   }

   private Map<String, Object> getCredentialsMap(String key) {
      return (Map<String, Object>) get(key);
   }

   public AuthCredentials getCredentials() {
      String type = getCredentialsType();

      return CredentialFactory.getCredentials(CredentialType.getCredentialType(type), getCredentialsMap(type));
   }
}
