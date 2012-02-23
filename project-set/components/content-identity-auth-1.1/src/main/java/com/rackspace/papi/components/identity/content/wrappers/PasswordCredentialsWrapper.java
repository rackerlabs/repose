package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "passwordCredentials")
public class PasswordCredentialsWrapper extends CredentialsWrapper<PasswordCredentials> {

   public void setPasswordCredentials(PasswordCredentials credentials) {
      setCredentials(credentials);
   }

   @Override
   public String getId() {
      return getCredentials() != null? getCredentials().getUsername(): null;
   }
   
   @Override
   public String getSecret() {
      return getCredentials() != null? getCredentials().getPassword(): null;
   }
   
}
