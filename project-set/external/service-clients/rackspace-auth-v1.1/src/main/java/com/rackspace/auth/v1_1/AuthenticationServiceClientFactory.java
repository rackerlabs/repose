package com.rackspace.auth.v1_1;

/**
 * @author fran
 */
public class AuthenticationServiceClientFactory {
    public AuthenticationServiceClient buildAuthServiceClient(String targetHostUri, String username, String password) {
        
        return new AuthenticationServiceClient(targetHostUri, new ResponseUnmarshaller(), new ServiceClient(username, password));
    }
}
