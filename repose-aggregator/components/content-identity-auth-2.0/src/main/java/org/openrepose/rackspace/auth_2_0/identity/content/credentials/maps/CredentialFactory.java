package org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers.ApiKeyCredentialsWrapper;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers.AuthenticationRequestWrapper;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers.PasswordCredentialsWrapper;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

import javax.xml.bind.JAXBElement;
import java.util.Map;

public final class CredentialFactory {

   private CredentialFactory() {
   }

   public static AuthCredentials getCredentials(CredentialType type, Map<String, Object> credentials) {
      // TODO
      return null;
   }

   public static <T> AuthCredentials getCredentials(JAXBElement<T> object) {
      AuthCredentials authCredentials = null;

      if (object != null && object.getValue() instanceof AuthenticationRequest) {
         AuthenticationRequest authRequest = (AuthenticationRequest) object.getValue();

         if (authRequest.getCredential() != null) {
            CredentialType credentialType = authRequest.getCredential().getValue();
            if (credentialType instanceof ApiKeyCredentials) {

               authCredentials = new ApiKeyCredentialsWrapper((ApiKeyCredentials) credentialType);
            } else if (credentialType instanceof PasswordCredentialsBase) {

               authCredentials = new PasswordCredentialsWrapper((PasswordCredentialsBase) credentialType);
            }
         } else {
            authCredentials = new AuthenticationRequestWrapper(authRequest);
         }
      }

      return authCredentials;
   }
}
