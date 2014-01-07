package org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers;

import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;

public class AuthenticationRequestWrapper implements AuthCredentials {

   private final org.openstack.docs.identity.api.v2.AuthenticationRequest authRequest;

   public AuthenticationRequestWrapper(org.openstack.docs.identity.api.v2.AuthenticationRequest authRequest) {
      this.authRequest = authRequest;
   }

   @Override
   public String getId() {
      return authRequest.getTenantName() != null ? authRequest.getTenantName() : authRequest.getTenantId();
   }
}
