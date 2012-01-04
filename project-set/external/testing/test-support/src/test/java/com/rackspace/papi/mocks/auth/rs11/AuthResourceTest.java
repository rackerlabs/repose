package com.rackspace.papi.mocks.auth.rs11;

import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import com.rackspacecloud.docs.auth.api.v1.Token;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import javax.ws.rs.core.Response.Status;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@Ignore
@RunWith(Enclosed.class)
public class AuthResourceTest {
   
   public static class WhenGettingTokens extends JerseyTest {
      private static final String baseResourcePackage = "com.rackspace.papi.mocks.auth.rs11";
      private final String INVALID_USER = "mickey";
      private final String VALID_USER = "cmarin1";
      private final String KEY = "SomeKey";
      private final String VALID_USER_TOKEN = "882210737:" + KEY.hashCode();
      
      public WhenGettingTokens() throws Exception {
         super(baseResourcePackage);
      }
      
      @Test
      public void shouldGetToken() {
         ClientResponse response = webResource
                 .path("/v1.1")
                 .header("X-Auth-User", VALID_USER)
                 .header("X-Auth-Key", KEY)
                 .get(ClientResponse.class);
         
         assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
         assertEquals(VALID_USER_TOKEN, response.getHeaders().getFirst("X-Auth-Token"));
      }
      
      @Test
      public void shouldValidateToken() {
         ClientResponse response = webResource
                 .path("/v1.1/token/" + VALID_USER_TOKEN)
                 .queryParam("belongsTo", VALID_USER)
                 .queryParam("type", "CLOUD")
                 .get(ClientResponse.class);
         
         assertEquals(Status.OK.getStatusCode(), response.getStatus());
         
         Token token = response.getEntity(Token.class);
         
         assertNotNull(token);
         assertEquals(VALID_USER_TOKEN, token.getId());
      }
      
      @Test
      public void shouldNotGetToken() {
         ClientResponse response = webResource
                 .path("/v1.1")
                 .header("X-Auth-User", INVALID_USER)
                 .header("X-Auth-Key", KEY)
                 .get(ClientResponse.class);
         
         assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      }

      @Test
      public void shouldNotValidateTokenForInvalidUser() {
         ClientResponse response = webResource
                 .path("/v1.1/token/" + VALID_USER_TOKEN)
                 .queryParam("belongsTo", INVALID_USER)
                 .queryParam("type", "CLOUD")
                 .get(ClientResponse.class);
         
         assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      }
      
      @Test
      public void shouldNotValidateInvalidTokenForValidUser() {
         ClientResponse response = webResource
                 .path("/v1.1/token/" + "GarbageToken")
                 .queryParam("belongsTo", VALID_USER)
                 .queryParam("type", "CLOUD")
                 .get(ClientResponse.class);
         
         assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      }
   }
   
   public static class WhenGettingGroups extends JerseyTest {
      private static final String baseResourcePackage = "com.rackspace.papi.mocks.auth.rs11";
      private final String INVALID_USER = "mickey";
      private final String VALID_USER = "cmarin1";
      private final String KEY = "SomeKey";
      private final String VALID_USER_TOKEN = "882210737:" + KEY.hashCode();
      
      public WhenGettingGroups() throws Exception {
         super(baseResourcePackage);
      }
      
      @Test
      public void getGroupsForValidUser() {
         ClientResponse response = webResource
                 .path("/v1.1/users/" + VALID_USER + "/groups")
                 .get(ClientResponse.class);
         
         assertEquals(Status.OK.getStatusCode(), response.getStatus());
         GroupsList groups = response.getEntity(GroupsList.class);
         
         assertNotNull(groups);
         assertEquals(1, groups.getGroup().size());
      }

      @Test
      public void getGroupsForInvalidUser() {
         ClientResponse response = webResource
                 .path("/v1.1/users/" + INVALID_USER + "/groups")
                 .get(ClientResponse.class);
         
         assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      }
   }
   
}
