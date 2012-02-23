package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "credentials")
public class UserCredentialsWrapper extends CredentialsWrapper<UserCredentials> {
   
   @Override
   public String getId() {
      return getCredentials() != null? getCredentials().getUsername(): null;
   }
   
   @Override
   public String getSecret() {
      return getCredentials() != null? getCredentials().getKey(): null;
   }
   
   public void setUserCredentials(UserCredentials credentials) {
      setCredentials(credentials);
   }
}
