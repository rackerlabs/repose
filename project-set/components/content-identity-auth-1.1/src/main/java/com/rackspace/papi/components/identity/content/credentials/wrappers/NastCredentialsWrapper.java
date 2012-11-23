package com.rackspace.papi.components.identity.content.credentials.wrappers;

import com.rackspacecloud.docs.auth.api.v1.NastCredentials;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement(name = "nastCredentials")
public class NastCredentialsWrapper extends CredentialsWrapper<NastCredentials> {

   private static final String[] FIELDS = {"nastId", "key"};
   
   public NastCredentialsWrapper() {
      super(FIELDS);
   }
   
   public NastCredentialsWrapper(Map map) {
      super(FIELDS);
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
