package com.rackspace.auth.openstack;


import com.rackspace.papi.service.authclient.akka.AkkaAuthenticationClient;
import com.rackspace.papi.service.httpclient.HttpClientService;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AuthenticationServiceFactoryTest {
    HttpClientService  httpClientService;
    AkkaAuthenticationClient akkaAuthenticationClient;

    /**
     * Test of build method, of class AuthenticationServiceFactory.
     */
    @Test
    public void testBuild() {
       AuthenticationServiceFactory instance = new AuthenticationServiceFactory();
       httpClientService=mock(HttpClientService.class);
       AuthenticationService result = instance.build("/some/host/uri", "username", "password",null,null,httpClientService,akkaAuthenticationClient);
       assertNotNull(result);
        
    }
    
}
