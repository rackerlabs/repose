package com.rackspace.papi.components.identity.content.wrappers;

import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mossoCredentials")
public class MossoCredentialsWrapper extends CredentialsWrapper<MossoCredentials> {

   public void setMossoCredentials(MossoCredentials credentials) {
      setCredentials(credentials);
   }

   @Override
   public String getId() {
      return getCredentials() != null? String.valueOf(getCredentials().getMossoId()): null;
   }
   
   @Override
   public String getSecret() {
      return getCredentials() != null? getCredentials().getKey(): null;
   }
   
}
