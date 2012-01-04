package com.rackspace.papi.mocks.auth.osids;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import java.io.IOException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.*;

@Ignore
@RunWith(Enclosed.class)
public class KeystoneResourceTest {
   
   public static class WhenGettingTokens extends JerseyTest {
      private static final String baseResourcePackage = "com.rackspace.papi.mocks.auth.osids";
      private final String INVALID_USER = "mickey";
      private final String VALID_USER = "cmarin2";
      private final String KEY = "SomeKey";
      private final String VALID_USER_TOKEN = VALID_USER.hashCode() + ":" + KEY.hashCode();
      private final String AUTH_TOKEN = "3005864:1075820758";
      private final ObjectFactory objectFactory = new ObjectFactory();
      
      public WhenGettingTokens() throws Exception {
         super(baseResourcePackage);
      }
      
      @Test
      public void shouldGetToken() throws JAXBException, ParserConfigurationException {
         AuthenticationRequest request = new AuthenticationRequest();
         TokenForAuthenticationRequest token = objectFactory.createTokenForAuthenticationRequest();
         token.setId(VALID_USER_TOKEN);
         
         PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
         credentials.setUsername(VALID_USER);
         credentials.setPassword(KEY);
         request.setCredential(objectFactory.createPasswordCredentials(credentials));
         request.setToken(token);

         ClientResponse response = webResource
                 .path("/keystone/tokens")
                 .type(MediaType.APPLICATION_XML)
                 .post(ClientResponse.class, objectFactory.createAuth(request));
         
         assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
         AuthenticateResponse entity = response.getEntity(AuthenticateResponse.class);
         assertNotNull(entity);
         
         assertEquals(token.getId(), entity.getToken().getId());
      }

      @Test
      public void shouldNotGetToken() throws JAXBException, ParserConfigurationException {
         AuthenticationRequest request = new AuthenticationRequest();
         TokenForAuthenticationRequest token = objectFactory.createTokenForAuthenticationRequest();
         token.setId(VALID_USER_TOKEN);
         
         PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
         credentials.setUsername(INVALID_USER);
         credentials.setPassword(KEY);
         request.setCredential(objectFactory.createPasswordCredentials(credentials));
         request.setToken(token);

         ClientResponse response = webResource
                 .path("/keystone/tokens")
                 .type(MediaType.APPLICATION_XML)
                 .post(ClientResponse.class, objectFactory.createAuth(request));
         
         assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
         UnauthorizedFault entity = response.getEntity(UnauthorizedFault.class);
         assertNotNull(entity);
         assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), entity.getCode());
         
      }
      
      @Test
      public void shouldValidateToken() {
         ClientResponse response = webResource
                 .path("/keystone/tokens/" + VALID_USER_TOKEN)
                 .header("X-Auth-Token", AUTH_TOKEN)
                 .get(ClientResponse.class);
         
         assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
         AuthenticateResponse entity = response.getEntity(AuthenticateResponse.class);
         assertNotNull(entity);
         
         assertEquals(VALID_USER_TOKEN, entity.getToken().getId());
         
      }

      @Test
      public void shouldNotValidateInvalidToken() {
         ClientResponse response = webResource
                 .path("/keystone/tokens/" + "Blah")
                 .header("X-Auth-Token", AUTH_TOKEN)
                 .get(ClientResponse.class);
         
         assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
         ItemNotFoundFault entity = response.getEntity(ItemNotFoundFault.class);
         assertNotNull(entity);
         
         assertEquals(Response.Status.NOT_FOUND.getStatusCode(), entity.getCode());
         
      }

      @Test
      public void shouldNotValidateTokenUsingInvalidAuthToken() {
         ClientResponse response = webResource
                 .path("/keystone/tokens/" + VALID_USER_TOKEN)
                 .header("X-Auth-Token", "Blah")
                 .get(ClientResponse.class);
         
         assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
         UnauthorizedFault entity = response.getEntity(UnauthorizedFault.class);
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
   }
}
