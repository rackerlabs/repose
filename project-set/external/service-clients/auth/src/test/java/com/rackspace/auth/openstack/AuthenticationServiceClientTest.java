package com.rackspace.auth.openstack;

import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class AuthenticationServiceClientTest {

    public static class TestParent {
        AuthenticationServiceClient authenticationServiceClient;
        ResponseUnmarshaller responseUnmarshaller;
        ServiceClientResponse<AuthenticateResponse> serviceClientResponseGet, serviceClientResponsePost;
        ServiceClient serviceClient;
        String tenant;
        String userToken;
        String targetHostUri;
        String username;
        String password;
        String tenantId;

        @Before
        public void setUp() throws Exception {
            AppenderForTesting.clear();
            tenant = "tenant";
            userToken = "userToken";
            targetHostUri = "targetHostUri";
            username = "username";
            password = "password";
            tenantId = "tenantId";
            responseUnmarshaller = mock(ResponseUnmarshaller.class);
            serviceClientResponseGet = mock(ServiceClientResponse.class);
            serviceClientResponsePost = mock(ServiceClientResponse.class);
            serviceClient = mock(ServiceClient.class);
            authenticationServiceClient =
                    new AuthenticationServiceClient(targetHostUri, username, password, tenantId, responseUnmarshaller,
                                                    responseUnmarshaller, serviceClient);
        }

        @After
        public void tearDown() throws Exception {
            AppenderForTesting.clear();
        }

        @Test
        public void shouldErrorWithCorrectMessageForInternalServerErrorCase() {
            when(serviceClient.get(anyString(), any(Map.class), anyString(), anyString()))
                    .thenReturn(serviceClientResponseGet);
            when(serviceClient.post(anyString(), any(JAXBElement.class), any(MediaType.class)))
                    .thenReturn(serviceClientResponsePost);
            when(serviceClientResponseGet.getStatusCode()).thenReturn(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
            when(serviceClientResponsePost.getStatusCode()).thenReturn(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());

            authenticationServiceClient.validateToken(tenant, userToken);

            assertTrue(AppenderForTesting.getMessages()[1]
                               .startsWith("Authentication Service returned internal server error:"));
        }

        @Test
        public void shouldErrorWithCorrectMessageForDefaultErrorCase() {
            when(serviceClient.get(anyString(), any(Map.class), anyString(), anyString()))
                    .thenReturn(serviceClientResponseGet);
            when(serviceClient.post(anyString(), any(JAXBElement.class), any(MediaType.class)))
                    .thenReturn(serviceClientResponsePost);
            when(serviceClientResponseGet.getStatusCode()).thenReturn(999);
            when(serviceClientResponsePost.getStatusCode()).thenReturn(999);

            authenticationServiceClient.validateToken(tenant, userToken);

            assertTrue(AppenderForTesting.getMessages()[1]
                               .startsWith("Authentication Service returned an unexpected response status code:"));
        }
    }
}
