package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement(name = "passwordCredentials")
public class PasswordCredentialsWrapper extends CredentialsWrapper<PasswordCredentials> {

   private static final String[] FIELDS = {"username", "password"};
   
   public PasswordCredentialsWrapper() {
      super(FIELDS);
   }
   
   public PasswordCredentialsWrapper(Map map) {
      super(FIELDS);
      validate(map);
      
      PasswordCredentials credentials = new PasswordCredentials();
      credentials.setUsername((String)map.get("username"));
      credentials.setPassword((String)map.get("password"));
      setCredentials(credentials);
   }
   
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
