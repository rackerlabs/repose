package com.rackspace.auth.openstack;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import com.rackspace.papi.service.httpclient.HttpClientService;

import static org.junit.Assert.assertNotNull;
import com.rackspace.papi.service.context.impl.HttpConnectionPoolServiceContext;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.mock;

public class AuthenticationServiceFactoryTest {
    HttpClientService  httpClientService;

    /**
     * Test of build method, of class AuthenticationServiceFactory.
     */
    @Test
    public void testBuild() {
       AuthenticationServiceFactory instance = new AuthenticationServiceFactory();
       httpClientService=mock(HttpClientService.class);
       AuthenticationService result = instance.build("/some/host/uri", "username", "password",null,null,httpClientService);
       assertNotNull(result);
        
    }
    
}
