package org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers;

import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "passwordCredentials")
public class PasswordCredentialsWrapper extends CredentialsWrapper<PasswordCredentialsBase> {

   public PasswordCredentialsWrapper(PasswordCredentialsBase passwordCredentialsBase) {
      setCredentials(passwordCredentialsBase);
   }

   @Override
   public String getId() {
      return getCredentials() != null ? getCredentials().getUsername() : null;
   }
}
