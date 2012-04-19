/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.reverseproxy.basicauth;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author malconis
 */
@RunWith(Enclosed.class)
public class ReverseProxyBasicAuthHandlerTest {

    public static class WhenAddingBackAuthCredentials {

        ReverseProxyBasicAuthHandler handler;
        private ReverseProxyBasicAuthConfig basicAuthConfig;
        private Credentials credentials;
        private final String username = "Aladdin";
        private final String password = "open sesame";
        private final String authHash = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==";
        private HttpServletRequest httpServletRequest;

        @Before
        public void setUp() {

            basicAuthConfig = new ReverseProxyBasicAuthConfig();
            credentials = new Credentials();

            credentials.setPassword(password);
            credentials.setUsername(username);

            basicAuthConfig.setCredentials(credentials);
            handler = new ReverseProxyBasicAuthHandler(basicAuthConfig);

            httpServletRequest = mock(HttpServletRequest.class);
        }

        @Test
        public void shouldSetAuthCredentialHeader() {
            FilterDirector director = new FilterDirectorImpl();
            
            director = handler.handleRequest(httpServletRequest, null);
            
            assertTrue(director.requestHeaderManager().headersToAdd().get("authorization").contains(authHash));
        }
    }
}
