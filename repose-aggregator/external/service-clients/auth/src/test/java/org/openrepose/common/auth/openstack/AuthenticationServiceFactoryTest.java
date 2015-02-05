package org.openrepose.common.auth.openstack;


import org.junit.Test;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AuthenticationServiceFactoryTest {
    HttpClientService  httpClientService;
    AkkaServiceClient akkaServiceClient;

    /**
     * Test of build method, of class AuthenticationServiceFactory.
     */
    @Test
    public void testBuild() throws Exception {
       AuthenticationServiceFactory instance = new AuthenticationServiceFactory();
       httpClientService=mock(HttpClientService.class);
       AuthenticationService result = instance.build("/some/host/uri", "username", "password",null,null,httpClientService, akkaServiceClient);
       assertNotNull(result);
    }
}
