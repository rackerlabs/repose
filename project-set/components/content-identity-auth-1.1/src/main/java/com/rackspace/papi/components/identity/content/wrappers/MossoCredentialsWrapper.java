package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mossoCredentials")
public class MossoCredentialsWrapper extends BaseCredentialsWrapper<MossoCredentials> {

   public void setMossoCredentials(MossoCredentials credentials) {
      setCredentials(credentials);
   }
}
