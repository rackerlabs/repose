package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(Enclosed.class)
public class AuthenticationServiceClientTest {

//     private final long NUMBER_OF_MILLISECONDS_IN_A_SECOND = 1000;
//        private final static long MOCK_CURRENT_SYS_TIME = 1000;

    //        private Calendar expirationTime;
//        private Calendar currentSystemTime;
    public static class WhenAuthenticating {
        private AuthenticationServiceClient authenticationServiceClient;
        private ServiceClient serviceClient;
        private ServiceClientResponse serviceClientResponse;
        private final String authEndpoint = "https://n01.endpoint.auth.rackspacecloud.com/v2.0";
        private ResponseUnmarshaller responseUnmarshaller;
        private final String inputUser = "345897";
        private final String inputToken = "aaaaa-aaaa-ddd-aaa-aaaa";
        private final String inputType = "MOSSO";
        private final String userId = "usertest1";
        private final String userGroup = "grouptest1";
        private final String groupDescription = "Test group Description";
        private ExtractorResult<String> result = new ExtractorResult<String>(inputUser, inputType);
        private final Map<String, String> headers = new HashMap<String, String>();

        @Before
        public void setup() throws JAXBException {

            serviceClient = mock(ServiceClient.class);
            serviceClientResponse = mock(ServiceClientResponse.class);

            JAXBContext jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);

            responseUnmarshaller = new ResponseUnmarshaller(jaxbContext);

            authenticationServiceClient = new AuthenticationServiceClient(authEndpoint, responseUnmarshaller, serviceClient);

            headers.put("Accept", MediaType.APPLICATION_XML);
        }

        @Test
        public void shouldReturnValidToken() {


            String response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><token xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\" "
                    + "created=\"2012-04-04T11:38:49.000-05:00\" userId=\"" + userId + "\" userURL=\"https://n01.endpoint.auth.rackspacecloud.com/v2.0/usertest1\" "
                    + "id=\"9f83bee1-305e-4ad6-8140-9c1e7e27200b\" expires=\"2012-04-05T11:38:49.000-05:00\"/>";

            InputStream data = new ByteArrayInputStream(response.getBytes());

            when(serviceClientResponse.getData()).thenReturn(data);
            when(serviceClientResponse.getStatusCode()).thenReturn(200);
            when(serviceClient.get(authEndpoint + "/token/" + inputToken, headers, "belongsTo", inputUser, "type", inputType)).thenReturn(serviceClientResponse);

            AuthToken tokenInfo = authenticationServiceClient.validateToken(result, inputToken);
            assertEquals(tokenInfo.getUserId(), userId);
        }

        @Test
        public void shouldReturnNullTokenOnBadToken() {
            when(serviceClientResponse.getStatusCode()).thenReturn(404);
            when(serviceClient.get(authEndpoint + "/token/" + inputToken, headers, "belongsTo", inputUser, "type", inputType)).thenReturn(serviceClientResponse);

            AuthToken tokenInfo = authenticationServiceClient.validateToken(result, inputToken);
            assertNull("No token should be returned on a 404", tokenInfo);
        }

        @Test
        public void shouldReturnNullTokenOnBadAdminToken() {
            when(serviceClientResponse.getStatusCode()).thenReturn(401);
            when(serviceClient.get(authEndpoint + "/token/" + inputToken, headers, "belongsTo", inputUser, "type", inputType)).thenReturn(serviceClientResponse);

            AuthToken tokenInfo = authenticationServiceClient.validateToken(result, inputToken);
            assertNull("No token should be returned on a 401", tokenInfo);
        }

        @Test
        public void shouldReturnUsersGroup() {
            String response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><groups"
                    + " xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"><group id=\"" + userGroup + "\">"
                    + "<description>Test group Description</description></group></groups>";

            InputStream data = new ByteArrayInputStream(response.getBytes());

            when(serviceClientResponse.getData()).thenReturn(data);
            when(serviceClientResponse.getStatusCode()).thenReturn(200);
            when(serviceClient.get(authEndpoint + "/users/" + userId + "/groups", headers)).thenReturn(serviceClientResponse);

            final AuthGroups authGroups = authenticationServiceClient.getGroups(userId);
            List<AuthGroup> groupsList = authGroups.getGroups();

            assertTrue("Groups list should contain users group", groupsList.get(0).getId().equals(userGroup));
            assertTrue(groupsList.get(0).getDescription().equals(groupDescription));

        }

        @Test
        public void shouldReturnNulAuthGroupsObject() {
            when(serviceClientResponse.getStatusCode()).thenReturn(404);
            when(serviceClient.get(authEndpoint + "/users/" + userId + "/groups", headers)).thenReturn(serviceClientResponse);

            final AuthGroups authGroups = authenticationServiceClient.getGroups(userId);

            assertNull("Should return null auth groups object", authGroups);

        }
        // TODO These tests needs to be moved to AuthTokenTest
        /*
       @Test
       public void shouldReturnMaxJavaInt() throws DatatypeConfigurationException {

       long mockExpirationTime = (MOCK_CURRENT_SYS_TIME + Integer.MAX_VALUE + 1l) * NUMBER_OF_MILLISECONDS_IN_A_SECOND;
       when(expirationTime.getTimeInMillis()).thenReturn(mockExpirationTime);

       assertEquals(Integer.MAX_VALUE, AuthenticationServiceClient.getTtl((GregorianCalendar)expirationTime, currentSystemTime));
       }

       @Test
       public void shouldReturnPositiveTime() throws DatatypeConfigurationException {

       long mockExpirationTime = MOCK_CURRENT_SYS_TIME + Integer.MAX_VALUE + 1l;
       when(expirationTime.getTimeInMillis()).thenReturn(mockExpirationTime);

       assertTrue(AuthenticationServiceClient.getTtl((GregorianCalendar)expirationTime, currentSystemTime) > 0);
       }
        *
        */
    }
}
