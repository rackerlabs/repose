package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mossoCredentials")
public class MossoCredentialsWrapper extends CredentialsWrapper<MossoCredentials> {

   private static final String[] fields = {"mossoId", "key"};
   
   public MossoCredentialsWrapper() {
      super(fields);
   }
   
   public MossoCredentialsWrapper(Map map) {
      super(fields);
      validate(map);
      
      MossoCredentials credentials = new MossoCredentials();
      credentials.setMossoId((Integer)map.get("mossoId"));
      credentials.setKey((String)map.get("key"));
      setCredentials(credentials);
   }
   
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
