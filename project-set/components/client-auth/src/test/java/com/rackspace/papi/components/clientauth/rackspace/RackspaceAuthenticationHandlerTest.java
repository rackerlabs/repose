package com.rackspace.papi.components.clientauth.rackspace;

import com.rackspace.papi.components.clientauth.rackspace.config.AuthenticationServer;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandler;
import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class RackspaceAuthenticationHandlerTest {
    public static class WhenGettingWWWAuthenticateHeaderContents {
        private RackspaceAuth authConfiguration;
        private CacheManager cacheManager;

        @Before
        public void setup() {
            cacheManager = new CacheManager();
            authConfiguration = new RackspaceAuth();
        }

        @Test
        public void shouldReturnRackspaceWWWAuthenticateHeaderContents() {
            String expected = "RackAuth Realm=\"API Realm\"";

            AuthenticationServer authenticationServer = new AuthenticationServer();
            authenticationServer.setUri("http://auth:8080/v1.1");
            authenticationServer.setUsername("authuser");
            authenticationServer.setPassword("authpassword");

            authConfiguration.setAuthenticationServer(authenticationServer);

            RackspaceAuthenticationHandler rackspaceAuthenticationModule = new RackspaceAuthenticationHandler(authConfiguration);

            String actual = rackspaceAuthenticationModule.getWWWAuthenticateHeaderContents();

            assertEquals(expected, actual);
        }
    }
}
