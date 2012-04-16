package org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.wrappers.xml.ApiKeyCredentialsWrapper;
import org.openstack.docs.identity.api.v2.CredentialType;

import java.util.Map;

public class CredentialFactory {

   private CredentialFactory() {
   }

   public static AuthCredentials getCredentials(CredentialType type, Map<String, Object> credentials) {
      // TODO
//        switch (type) {
//            case MOSSO:
//                return new MossoCredentialsWrapper(credentials);
//            case NAST:
//                return new NastCredentialsWrapper(credentials);
//            case PASSWORD:
//                return new PasswordCredentialsWrapper(credentials);
//            case USER:
//                return new UserCredentialsWrapper(credentials);
//        }

      return null;
   }

   public static <T> AuthCredentials getCredentials(T credentials) {
      AuthCredentials authCredentials = null;

      if (credentials == null) {
         int i = -1;         
      }

//      if (credentials != null) {
//         if (credentials.getCredential() != null) {
//            if (credentials.getCredential().getValue() instanceof ApiKeyCredentials) {
//
//               ApiKeyCredentialsWrapper credentialsWrapper = new ApiKeyCredentialsWrapper();
//               credentialsWrapper.setMossoCredentials((ApiKeyCredentialsWrapper) credentials.getCredential().getValue());
//               authCredentials = credentialsWrapper;
//            } else if (credentials instanceof NastCredentials) {
//
//               NastCredentialsWrapper credentialsWrapper = new NastCredentialsWrapper();
//               credentialsWrapper.setNastCredentials((NastCredentials) credentials);
//               authCredentials = credentialsWrapper;
//            }
//         }
//
//
//      }

      return authCredentials;
   }
}
