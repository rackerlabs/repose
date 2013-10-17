package com.rackspace.papi.components.identity.content.credentials.maps;

public enum CredentialType {

   MOSSO("mossoCredentials"),
   NAST("nastCredentials"),
   PASSWORD("passwordCredentials"),
   USER("credentials"),
   UNKNOWN("other");

   CredentialType(String name) {
      this.name = name;
   }
   private String name;

   public static CredentialType getCredentialType(String name) {
      for (CredentialType type : CredentialType.values()) {
         if (type.name.equals(name)) {
            return type;
         }
      }

      return UNKNOWN;
   }
}
