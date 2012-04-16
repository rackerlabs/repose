package org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers.xml;

import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

public class XmlAuthRequest implements AuthCredentials {

   private final AuthenticationRequest authRequest;

   public XmlAuthRequest(AuthenticationRequest authRequest) {
      this.authRequest = authRequest;
   }

   @Override
   public String getId() {

      if (authRequest.getCredential() == null) {
         
      }

      return null;
   }
}
