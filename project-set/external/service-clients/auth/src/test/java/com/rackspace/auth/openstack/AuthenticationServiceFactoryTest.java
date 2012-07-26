package com.rackspace.auth.openstack;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class AuthenticationServiceFactoryTest {

    public static class WhenBuilding {

        @Test
        public void shouldBuildAthenticationService() {
            final AuthenticationService service = new AuthenticationServiceFactory().build("/some/host/uri", "username", "password");

            assertNotNull(service);
        }
    }
    
}
