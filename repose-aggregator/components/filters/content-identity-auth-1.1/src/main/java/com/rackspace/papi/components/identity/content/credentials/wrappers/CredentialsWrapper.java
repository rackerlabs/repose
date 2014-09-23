package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspacecloud.docs.auth.api.v1.Credentials;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class CredentialsWrapper<T extends Credentials> implements AuthCredentials {
   public static final List<Class<? extends CredentialsWrapper>> WRAPPERS = new ArrayList<Class<? extends CredentialsWrapper>>();
   static {
      WRAPPERS.add(UserCredentialsWrapper.class);
      WRAPPERS.add(MossoCredentialsWrapper.class);
      WRAPPERS.add(NastCredentialsWrapper.class);
      WRAPPERS.add(PasswordCredentialsWrapper.class);
   };
   
   private T credentials;
   private final List<String> fields;
   
   public CredentialsWrapper() {
      fields = null;
   }
   
   public CredentialsWrapper(String[] fields) {
      this.fields = Arrays.asList(fields);
   }
   
   
   public void validate(Map map) {
      if (fields == null) {
         return;
      }
      
      for (String field: fields) {
         if (!map.containsKey(field)) {
            throw new IllegalArgumentException("Field required in map: " + field);
         }
      }
   }
   
   public void setCredentials(T credentials) {
      this.credentials = credentials;
   }
   
   public T getCredentials() {
      return credentials;
   }
   
   public abstract String getId();
   
   public abstract String getSecret();
   
}
