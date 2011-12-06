package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.v1_1.Account;
import com.rackspace.auth.v1_1.AuthenticationServiceClient;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountType;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspacecloud.docs.auth.api.v1.Group;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RackspaceAuthenticationHandlerTest {

    public static class WhenAuthenticating {

        private AuthenticationServiceClient authServiceClient;
        private HttpServletRequest request;
        private RackspaceAuthenticationHandler authHandler;
        private GroupsList groups;

        @Before
        public void standUp() {
            final RackspaceAuth authConfig = new RackspaceAuth();

            // Add a new account mapping to the account mapping list
            final AccountMapping accountUriMapping = new AccountMapping();
            accountUriMapping.setIdRegex(".*/([\\d]+)/.*");
            accountUriMapping.setType(AccountType.MOSSO);

            authConfig.getAccountMapping().add(accountUriMapping);

            // Add a default group to the groups list
            groups = new GroupsList();
            
            final Group defaultGroup = new Group();
            defaultGroup.setDescription("A default group");
            defaultGroup.setId("group-id");
            
            groups.getGroup().add(defaultGroup);
            
            // Mocking fun
            authServiceClient = mock(AuthenticationServiceClient.class);
            authHandler = new RackspaceAuthenticationHandler(authConfig, authServiceClient);

            // Mocking out the request
            request = mock(HttpServletRequest.class);
            when(request.getHeader(CommonHttpHeader.AUTH_TOKEN.getHeaderKey())).thenReturn("aaaa-aaaa-aaaa-aaaa-aaaaaaaa");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://ponies.com/12345/ponies"));
        }

        @Test
        public void shouldReturnFilterDirector() {
            assertNotNull("When authenticating correctly, a non-null filter directors must be returned", authHandler.authenticate(request));
        }

        @Test
        public void shouldValidateUserAuthToken() {
            when(authServiceClient.validateToken(any(Account.class), anyString())).thenReturn(Boolean.TRUE);
            when(authServiceClient.getGroups(anyString())).thenReturn(groups);
            
            final FilterDirector actual = authHandler.authenticate(request);
            
            assertNotNull("When authenticating correctly, a non-null filter directors must be returned", actual);
            assertEquals("When authenticating correctly, a valid token must pass", FilterAction.PASS, actual.getFilterAction());

            verify(authServiceClient, times(1)).validateToken(any(Account.class), anyString());
        }
        
        @Test
        public void shouldHandleAuthenticationServiceFailures() {
            when(authServiceClient.validateToken(any(Account.class), anyString())).thenThrow(new RuntimeException());
                    
            assertEquals("When auth service fails, repose should return a 500", HttpStatusCode.INTERNAL_SERVER_ERROR, authHandler.authenticate(request).getResponseStatus());

            verify(authServiceClient, times(1)).validateToken(any(Account.class), anyString());
        }
        
        @Test
        public void shouldRetrieveGroupsWhenTokenIsValid() {
            when(authServiceClient.validateToken(any(Account.class), anyString())).thenReturn(Boolean.TRUE);
            when(authServiceClient.getGroups(anyString())).thenReturn(groups);
            
//                        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.getHeaderKey(), groups);
//            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.getHeaderKey(), accountUsername);
            final FilterDirector actual = authHandler.authenticate(request);
            final Map<String, Set<String>> headersAdded = actual.requestHeaderManager().headersToAdd();
            
            assertTrue("When groups exist, handler must add X-PP-Groups header", headersAdded.get(PowerApiHeader.GROUPS.getHeaderKey().toLowerCase()).contains("group-id"));
            assertTrue("When groups exist, handler must add X-PP-Groups header", headersAdded.get(PowerApiHeader.USER.getHeaderKey().toLowerCase()).contains("12345"));
        }
    }
}
