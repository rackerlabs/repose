package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "credentials")
public class UserCredentialsWrapper extends CredentialsWrapper<UserCredentials> {
   
   private static final String[] fields = {"username", "key"};
   
   public UserCredentialsWrapper() {
      super(fields);
   }
   
   public UserCredentialsWrapper(Map map) {
      super(fields);
      validate(map);
      
      UserCredentials credentials = new UserCredentials();
      credentials.setUsername((String)map.get("username"));
      credentials.setKey((String)map.get("key"));
      setCredentials(credentials);
   }
   
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
