package org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps;

public enum Credential {

   API_KEY("apiKeyCredentials"),
   PASSWORD("passwordCredentials"),
   USER("credentials"),
   UNKNOWN("other");

   Credential(String name) {
      this.name = name;
   }
   private String name;

   public static Credential getCredential(String name) {
      for (Credential type : Credential.values()) {
         if (type.name.equals(name)) {
            return type;
         }
      }

      return UNKNOWN;
   }
}
