package com.rackspace.papi.mocks.auth.osids;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class KeystoneResourceTest {

    @Ignore
    public static class TestParent {

        public static final String INVALID_USER = "mickey";
        public static final String VALID_USER = "cmarin2";
        public static final String KEY = "SomeKey";
        public static final String VALID_USER_TOKEN = VALID_USER.hashCode() + ":" + KEY.hashCode();
        public static final String AUTH_TOKEN = "3005864:1075820758";
        public static final ObjectFactory objectFactory = new ObjectFactory();
        protected KeystoneResource keystoneResource;

        @Before
        public void beforeAll() throws Exception {
            keystoneResource = new KeystoneResource();
        }

        public static <T> T getEntity(Response r) {
            return ((JAXBElement<T>) r.getEntity()).getValue();
        }
    }

    public static class WhenGettingTokens extends TestParent {

        @Test
        public void shouldGetToken() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest();
            TokenForAuthenticationRequest token = objectFactory.createTokenForAuthenticationRequest();
            token.setId(VALID_USER_TOKEN);

            PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
            credentials.setUsername(VALID_USER);
            credentials.setPassword(KEY);
            request.setCredential(objectFactory.createPasswordCredentials(credentials));
            request.setToken(token);

            UriInfo uriInfo = mock(UriInfo.class);

            Response response = keystoneResource.getToken(request, uriInfo);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            AuthenticateResponse entity = getEntity(response);

            assertNotNull(entity);
            assertEquals(token.getId(), entity.getToken().getId());
        }

        @Test
        public void shouldNotGetToken() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest();
            TokenForAuthenticationRequest token = objectFactory.createTokenForAuthenticationRequest();
            token.setId(VALID_USER_TOKEN);

            PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
            credentials.setUsername(INVALID_USER);
            credentials.setPassword(KEY);
            request.setCredential(objectFactory.createPasswordCredentials(credentials));
            request.setToken(token);

            UriInfo uriInfo = mock(UriInfo.class);
            Response response = keystoneResource.getToken(request, uriInfo);

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            UnauthorizedFault entity = getEntity(response);
            assertNotNull(entity);
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), entity.getCode());
        }

        @Test
        public void shouldValidateToken() {
            UriInfo uriInfo = mock(UriInfo.class);
            Response response = keystoneResource.validateToken(VALID_USER_TOKEN, AUTH_TOKEN, VALID_USER, uriInfo);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            AuthenticateResponse entity = getEntity(response);
            assertNotNull(entity);

            assertEquals(VALID_USER_TOKEN, entity.getToken().getId());
        }

        @Test
        public void shouldNotValidateInvalidToken() {
            UriInfo uriInfo = mock(UriInfo.class);
            Response response = keystoneResource.validateToken("Blah", AUTH_TOKEN, VALID_USER, uriInfo);

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            ItemNotFoundFault entity = getEntity(response);
            assertNotNull(entity);

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), entity.getCode());

        }

        @Test
        public void shouldNotValidateTokenUsingInvalidAuthToken() {
            UriInfo uriInfo = mock(UriInfo.class);
            Response response = keystoneResource.validateToken(VALID_USER_TOKEN, "Blah", VALID_USER, uriInfo);

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            UnauthorizedFault entity = getEntity(response);
            assertNotNull(entity);

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), entity.getCode());
        }

        @Test
        public void shouldValidateCredentials() throws DatatypeConfigurationException, IOException {
            PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
            credentials.setUsername(VALID_USER);
            credentials.setPassword(KEY);

            KeystoneResource instance = new KeystoneResource("/test_keystone.properties");
            assertTrue(instance.validateCredentials(credentials));

        }

        @Test
        public void shouldNotValidateInvalidCredentials() throws DatatypeConfigurationException, IOException {
            PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
            credentials.setUsername(INVALID_USER);
            credentials.setPassword(KEY);

            KeystoneResource instance = new KeystoneResource("/test_keystone.properties");
            assertFalse(instance.validateCredentials(credentials));

        }

        @Test
        public void shouldNotValidateValidTokenWithInvalidUser() throws DatatypeConfigurationException, IOException {
            UriInfo uriInfo = mock(UriInfo.class);
            Response response = keystoneResource.validateToken(VALID_USER_TOKEN, AUTH_TOKEN, INVALID_USER, uriInfo);

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            ItemNotFoundFault entity = getEntity(response);
            assertNotNull(entity);

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), entity.getCode());
        }
    }
}
