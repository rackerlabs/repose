package com.rackspace.auth.openstack;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;

public class AuthenticationServiceFactoryTest {

    /**
     * Test of build method, of class AuthenticationServiceFactory.
     */
    @Test
    public void testBuild() {
       AuthenticationServiceFactory instance = new AuthenticationServiceFactory();
       AuthenticationService result = instance.build("/some/host/uri", "username", "password",null);
       assertNotNull(result);
        
    }
    
}
