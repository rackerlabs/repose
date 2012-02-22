package com.rackspace.papi.components.identity.content.wrappers;

public class BaseCredentialsWrapper<T> {
   private T credentials;
   
   public void setCredentials(T credentials) {
      this.credentials = credentials;
   }
   
   public T getCredentials() {
      return credentials;
   }
}
