package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "nastCredentials")
public class NastCredentialsWrapper extends CredentialsWrapper<NastCredentials> {
   public void setNastCredentials(NastCredentials credentials) {
      setCredentials(credentials);
   }

   @Override
   public String getId() {
      return getCredentials() != null? getCredentials().getNastId(): null;
   }
   
   @Override
   public String getSecret() {
      return getCredentials() != null? getCredentials().getKey(): null;
   }
   
}
