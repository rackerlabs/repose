package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "credentials")
public class UserCredentialsWrapper extends BaseCredentialsWrapper<UserCredentials> {
   public void setUserCredentials(UserCredentials credentials) {
      setCredentials(credentials);
   }
}
