package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "credentials")
public class NastCredentialsWrapper extends BaseCredentialsWrapper<NastCredentials> {
   public void setNastCredentials(NastCredentials credentials) {
      setCredentials(credentials);
   }
}
