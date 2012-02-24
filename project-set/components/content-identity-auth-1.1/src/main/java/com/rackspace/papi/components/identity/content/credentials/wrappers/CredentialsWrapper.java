package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CredentialsWrapper<T extends Credentials> implements AuthCredentials {
   public final static List<Class<? extends CredentialsWrapper>> wrappers = new ArrayList<Class<? extends CredentialsWrapper>>();
   static {
      wrappers.add(UserCredentialsWrapper.class);
      wrappers.add(MossoCredentialsWrapper.class);
      wrappers.add(NastCredentialsWrapper.class);
      wrappers.add(PasswordCredentialsWrapper.class);
   };
   
   private T credentials;
   private final String[] fields;
   
   public CredentialsWrapper() {
      fields = null;
   }
   
   public CredentialsWrapper(String[] fields) {
      this.fields = fields;
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
