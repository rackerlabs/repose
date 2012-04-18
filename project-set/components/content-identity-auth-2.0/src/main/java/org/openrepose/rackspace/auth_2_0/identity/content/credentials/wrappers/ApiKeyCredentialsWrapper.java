package org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "apiKeyCredentials")
public class ApiKeyCredentialsWrapper extends CredentialsWrapper<ApiKeyCredentials>{

   public ApiKeyCredentialsWrapper(ApiKeyCredentials credentials) {
      setCredentials(credentials);
   }

   @Override
   public String getId() {
      return getCredentials() != null ? getCredentials().getUsername() : null;
   }
}
