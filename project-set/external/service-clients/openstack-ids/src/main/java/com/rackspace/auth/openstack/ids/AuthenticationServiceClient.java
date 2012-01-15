package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

import java.util.Calendar;
import java.util.List;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.Token;

/**
 * @author fran
 */
public class AuthenticationServiceClient implements OpenStackAuthenticationService {

   private final String targetHostUri;
   private final GenericServiceClient serviceClient;
   private final OpenStackCoreResponseUnmarshaller openStackCoreResponseUnmarshaller;
   private final OpenStackGroupsResponseUnmarshaller openStackGroupsResponseUnmarshaller;
   private AdminToken currentAdminToken;

   private static class AdminToken {

      private final String token;
      private final Calendar expires;

      public AdminToken(String token, Calendar expires) {
         this.token = token;
         this.expires = expires;
      }

      public String getToken() {
         return token;
      }

      public boolean isValid() {
         return expires.before(Calendar.getInstance().getTime());
      }
   }

   public AuthenticationServiceClient(String targetHostUri, String username, String password) {
      this.openStackCoreResponseUnmarshaller = new OpenStackCoreResponseUnmarshaller();
      this.openStackGroupsResponseUnmarshaller = new OpenStackGroupsResponseUnmarshaller();
      this.serviceClient = new GenericServiceClient(username, password);
      this.targetHostUri = targetHostUri;
   }

   @Override
   public CachableUserInfo validateToken(String tenant, String userToken) {
      CachableUserInfo token = null;

      final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, getAdminToken());

      switch (serviceResponse.getStatusCode()) {
         case 200:
            final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
            token = new CachableUserInfo(authenticateResponse);
      }

      return token;
   }

   @Override
   public List<Endpoint> getEndpointsForToken(String userToken) {
      final ServiceClientResponse<EndpointList> endpointListResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken + "/endpoints", getAdminToken());
      List<Endpoint> endpointList = null;

      switch (endpointListResponse.getStatusCode()) {
         case 200:
            final EndpointList unmarshalledEndpoints = openStackCoreResponseUnmarshaller.unmarshall(endpointListResponse.getData(), EndpointList.class);

            if (unmarshalledEndpoints != null) {
               endpointList = unmarshalledEndpoints.getEndpoint();
            }
      }

      return endpointList;
   }

   @Override
   public Groups getGroups(String userId) {
      final ServiceClientResponse<Groups> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", getAdminToken());
      Groups groups = null;

      switch (serviceResponse.getStatusCode()) {
         case 200:
            groups = openStackGroupsResponseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);
      }

      return groups;
   }

   private String getAdminToken() {
      String adminToken = currentAdminToken != null && currentAdminToken.isValid() ? currentAdminToken.getToken() : null;

      if (adminToken == null) {
         final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.getAdminToken(targetHostUri + "/tokens");

         switch (serviceResponse.getStatusCode()) {
            case 200:
               final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);

               Token token = authenticateResponse.getToken();
               currentAdminToken = new AdminToken(token.getId(), token.getExpires().toGregorianCalendar());
               adminToken = currentAdminToken.getToken();
         }
      }

      return adminToken;
   }
}
