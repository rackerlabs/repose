package com.rackspace.auth.openstack;


import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.services.httpclient.HttpClientService;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AuthenticationServiceFactoryTest {
    HttpClientService  httpClientService;
    AkkaServiceClient akkaServiceClient;

    /**
     * Test of build method, of class AuthenticationServiceFactory.
     */
    @Test
    public void testBuild() {
       AuthenticationServiceFactory instance = new AuthenticationServiceFactory();
       httpClientService=mock(HttpClientService.class);
       AuthenticationService result = instance.build("/some/host/uri", "username", "password",null,null,httpClientService, akkaServiceClient);
       assertNotNull(result);
        
    }
    
}
