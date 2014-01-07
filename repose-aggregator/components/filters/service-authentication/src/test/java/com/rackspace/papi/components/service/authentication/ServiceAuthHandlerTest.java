package com.rackspace.papi.components.service.authentication;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author malconis
 */
@RunWith(Enclosed.class)
public class ServiceAuthHandlerTest {

    public static class WhenAddingBackAuthCredentials {

        ServiceAuthHandler handler;
        private ServiceAuthenticationConfig basicAuthConfig;
        private Credentials credentials;
        private final String username = "Aladdin";
        private final String password = "open sesame";
        private final String authHash = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==";
        private HttpServletRequest httpServletRequest;
        private ReadableHttpServletResponse httpServletResponse;
        private ServiceAuthHandlerFactory factory;

        @Before
        public void setUp() {

            basicAuthConfig = new ServiceAuthenticationConfig();
            factory = new ServiceAuthHandlerFactory();
            credentials = new Credentials();
            

            credentials.setPassword(password);
            credentials.setUsername(username);

            basicAuthConfig.setCredentials(credentials);
            factory.configurationUpdated(basicAuthConfig);
            
            handler = factory.buildHandler();

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
