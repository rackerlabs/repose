package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "nastCredentials")
public class NastCredentialsWrapper extends CredentialsWrapper<NastCredentials> {

   private static final String[] fields = {"nastId", "key"};
   
   public NastCredentialsWrapper() {
      super(fields);
   }
   
   public NastCredentialsWrapper(Map map) {
      super(fields);
      validate(map);
      
      NastCredentials credentials = new NastCredentials();
      credentials.setNastId((String)map.get("nastId"));
      credentials.setKey((String)map.get("key"));
      setCredentials(credentials);
   }
   
   
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
