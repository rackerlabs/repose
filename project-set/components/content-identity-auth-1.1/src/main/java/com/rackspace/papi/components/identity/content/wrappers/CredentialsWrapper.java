package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import java.util.ArrayList;
import java.util.List;

public abstract class CredentialsWrapper<T extends Credentials> {
   public final static List<Class<? extends CredentialsWrapper>> wrappers = new ArrayList<Class<? extends CredentialsWrapper>>();
   static {
      wrappers.add(UserCredentialsWrapper.class);
      wrappers.add(MossoCredentialsWrapper.class);
      wrappers.add(NastCredentialsWrapper.class);
      wrappers.add(PasswordCredentialsWrapper.class);
   };
   
   private T credentials;
   
   public void setCredentials(T credentials) {
      this.credentials = credentials;
   }
   
   public T getCredentials() {
      return credentials;
   }
   
   public abstract String getId();
   
   public abstract String getSecret();
   
}
