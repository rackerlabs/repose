package com.rackspace.papi.mocks.auth.rs11;

import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import com.rackspacecloud.docs.auth.api.v1.Token;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@Ignore
@RunWith(Enclosed.class)
public class AuthResourceTest {

   @Ignore
   public static class TestParent {

      public static final String INVALID_USER = "mickey";
      public static final String VALID_USER = "cmarin1";
      public static final String KEY = "SomeKey";
      public static final String VALID_USER_TOKEN = "882210737:" + KEY.hashCode();
      
      protected AuthResource authResource;

      @Before
      public void beforeAll() throws Exception {
         authResource = new AuthResource();
      }

      public static <T> T getEntity(Response r) {
         return ((JAXBElement<T>) r.getEntity()).getValue();
      }

      public static String getHeader(Response r, String headerName) {
         return (String) r.getMetadata().getFirst(headerName);
      }
   }

   public static class WhenGettingTokens extends TestParent {

      @Test
      public void shouldGetToken() {
         Response response = authResource.getToken(VALID_USER, KEY);

         assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
         assertEquals(VALID_USER_TOKEN, getHeader(response, "X-Auth-Token"));
      }

      @Test
      public void shouldValidateToken() throws Exception {
         UriInfo uriInfo = mock(UriInfo.class);
         Response response = authResource.validateToken(VALID_USER_TOKEN, VALID_USER, "CLOUD", uriInfo);

         assertEquals(Status.OK.getStatusCode(), response.getStatus());

         Token token = getEntity(response);

         assertNotNull(token);
         assertEquals(VALID_USER_TOKEN, token.getId());
      }

      @Test
      public void shouldNotGetToken() {
         Response response = authResource.getToken(INVALID_USER, KEY);

         assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      }

      @Test
      public void shouldNotValidateTokenForInvalidUser() throws Exception {
         UriInfo uriInfo = mock(UriInfo.class);
         Response response = authResource.validateToken(VALID_USER_TOKEN, INVALID_USER, "CLOUD", uriInfo);

         assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      }

      @Test
      public void shouldNotValidateInvalidTokenForValidUser() throws Exception {
         UriInfo uriInfo = mock(UriInfo.class);
         Response response = authResource.validateToken("GarbageToken", VALID_USER, "CLOUD", uriInfo);

         assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      }
   }

   public static class WhenGettingGroups extends TestParent {

      @Test
      public void getGroupsForValidUser() {
         Response response = authResource.getGroups(VALID_USER);

         assertEquals(Status.OK.getStatusCode(), response.getStatus());
         GroupsList groups = getEntity(response);

         assertNotNull(groups);
         assertEquals(1, groups.getGroup().size());
      }

      @Test
      public void getGroupsForInvalidUser() {
         Response response = authResource.getGroups(INVALID_USER);

         assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      }
   }
}
