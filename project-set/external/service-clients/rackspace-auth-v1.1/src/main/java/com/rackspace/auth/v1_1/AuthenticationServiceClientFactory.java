package com.rackspace.auth.v1_1;

import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.rackspace.papi.commons.util.http.ServiceClient;

/**
 * @author fran
 */
public class AuthenticationServiceClientFactory {
   public AuthenticationServiceClient buildAuthServiceClient(String targetHostUri, String username, String password) {
      HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);

      return new AuthenticationServiceClient(targetHostUri, new ResponseUnmarshaller(), new ServiceClient(authFilter));
   }
}
