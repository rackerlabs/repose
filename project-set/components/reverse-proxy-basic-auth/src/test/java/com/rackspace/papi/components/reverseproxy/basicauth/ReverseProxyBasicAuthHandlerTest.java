/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.reverseproxy.basicauth;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        private ReadableHttpServletResponse httpServletResponse;

        @Before
        public void setUp() {

            basicAuthConfig = new ReverseProxyBasicAuthConfig();
            credentials = new Credentials();

            credentials.setPassword(password);
            credentials.setUsername(username);

            basicAuthConfig.setCredentials(credentials);
            handler = new ReverseProxyBasicAuthHandler(basicAuthConfig);

            httpServletRequest = mock(HttpServletRequest.class);
            httpServletResponse = mock(ReadableHttpServletResponse.class);
        }

        @Test
        public void shouldSetAuthCredentialHeader() {
            FilterDirector director = new FilterDirectorImpl();

            director = handler.handleRequest(httpServletRequest, null);

            assertTrue(director.requestHeaderManager().headersToAdd().get("authorization").contains(authHash));
        }

        @Test
        public void shouldReturn500On501() {

            FilterDirector director = new FilterDirectorImpl();

            when(httpServletResponse.getStatus()).thenReturn(HttpStatusCode.NOT_IMPLEMENTED.intValue());
            director = handler.handleResponse(httpServletRequest, httpServletResponse);

            assertEquals(director.getResponseStatus(), HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        @Test
        public void shouldReturn500On403() {

            FilterDirector director = new FilterDirectorImpl();

            when(httpServletResponse.getStatus()).thenReturn(HttpStatusCode.FORBIDDEN.intValue());
            director = handler.handleResponse(httpServletRequest, httpServletResponse);

            assertEquals(director.getResponseStatus(), HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        @Test
        public void shouldSetAuthenticateHeaderToRemove() {
            FilterDirector director = new FilterDirectorImpl();

            when(httpServletResponse.getStatus()).thenReturn(HttpStatusCode.FORBIDDEN.intValue());
            when(httpServletResponse.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("delegated");
            director = handler.handleResponse(httpServletRequest, httpServletResponse);

            assertTrue(director.responseHeaderManager().headersToRemove().contains(CommonHttpHeader.WWW_AUTHENTICATE.toString()));
            assertEquals(director.getResponseStatus(), HttpStatusCode.FORBIDDEN);
        }

        @Test
        public void shouldSetAuthenticateHeaderToRemoveOn501() {

            FilterDirector director = new FilterDirectorImpl();

            when(httpServletResponse.getStatus()).thenReturn(HttpStatusCode.NOT_IMPLEMENTED.intValue());
            when(httpServletResponse.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("delegated");
            director = handler.handleResponse(httpServletRequest, httpServletResponse);
            

            assertTrue(director.responseHeaderManager().headersToRemove().contains(CommonHttpHeader.WWW_AUTHENTICATE.toString()));
            assertEquals(director.getResponseStatus(), HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
    }
}
