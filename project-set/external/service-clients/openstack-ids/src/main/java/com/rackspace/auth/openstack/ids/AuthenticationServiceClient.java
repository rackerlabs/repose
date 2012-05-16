package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openstack.docs.identity.api.v2.*;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

/**
 * @author fran
 */
public class AuthenticationServiceClient implements OpenStackAuthenticationService {

   private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClient.class);
   private final String targetHostUri;
   private final GenericServiceClient serviceClient;
   private final OpenStackCoreResponseUnmarshaller openStackCoreResponseUnmarshaller;
   private final OpenStackGroupsResponseUnmarshaller openStackGroupsResponseUnmarshaller;
   private AdminToken currentAdminToken;
   private final JAXBElement jaxbRequest;

   public AuthenticationServiceClient(String targetHostUri, String username, String password) {
      this.openStackCoreResponseUnmarshaller = new OpenStackCoreResponseUnmarshaller();
      this.openStackGroupsResponseUnmarshaller = new OpenStackGroupsResponseUnmarshaller();
      this.serviceClient = new GenericServiceClient();
      this.targetHostUri = targetHostUri;

      ObjectFactory objectFactory = new ObjectFactory();
      PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
      credentials.setUsername(username);
      credentials.setPassword(password);

      JAXBElement jaxbCredentials = objectFactory.createPasswordCredentials(credentials);

      AuthenticationRequest request = new AuthenticationRequest();
      request.setCredential(jaxbCredentials);

      this.jaxbRequest = objectFactory.createAuth(request);
   }

   @Override
   public CachableUserInfo validateToken(String tenant, String userToken) {
      CachableUserInfo token = null;
      
      final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, getAdminToken(), "belongsTo", tenant);

      switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
         case OK:
            final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
            token = new CachableUserInfo(tenant, authenticateResponse);
            break;
            
         case NOT_FOUND: // User's token is bad
            LOG.warn("Unable to validate token for tenant.  Invalid token. " + serviceResponse.getStatusCode());
            break;
            
         case UNAUTHORIZED: // Admin token is bad most likely
            LOG.warn("Unable to validate token for tenant.  Has the admin token expired? " + serviceResponse.getStatusCode());
            break;

         case INTERNAL_SERVER_ERROR: // Internal server error from auth
            LOG.warn("Internal server error from auth. " + serviceResponse.getStatusCode());
            break;
      }

      return token;
   }

   @Override
   public List<Endpoint> getEndpointsForToken(String userToken) {
      final ServiceClientResponse<EndpointList> endpointListResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken + "/endpoints", getAdminToken());
      List<Endpoint> endpointList = null;

      switch (HttpStatusCode.fromInt(endpointListResponse.getStatusCode())) {
         case OK:
            final EndpointList unmarshalledEndpoints = openStackCoreResponseUnmarshaller.unmarshall(endpointListResponse.getData(), EndpointList.class);

            if (unmarshalledEndpoints != null) {
               endpointList = unmarshalledEndpoints.getEndpoint();
            }
            
            break;
            
         default:
            LOG.warn("Unable to get endpoints for token: " + endpointListResponse.getStatusCode());
            break;
      }

      return endpointList;
   }

   @Override
   public Groups getGroups(String userId) {
      final ServiceClientResponse<Groups> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", getAdminToken());
      Groups groups = null;

      switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
         case OK:
            groups = openStackGroupsResponseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);
            break;
            
         default:
            LOG.warn("Unable to get groups for user id: " + serviceResponse.getStatusCode());
            break;
            
      }

      return groups;
   }

   private synchronized String getAdminToken() {
      String adminToken = currentAdminToken != null && currentAdminToken.isValid() ? currentAdminToken.getToken() : null;

      if (adminToken == null) {
         final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.post(targetHostUri + "/tokens", jaxbRequest, MediaType.APPLICATION_XML_TYPE);

         switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
            case OK:
               final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);

               Token token = authenticateResponse.getToken();
               currentAdminToken = new AdminToken(token.getId(), token.getExpires().toGregorianCalendar());
               adminToken = currentAdminToken.getToken();
               break;
               
            default:
               LOG.error("Unable to get admin token.  Verify admin credentials. " + serviceResponse.getStatusCode());
               break;
         }
      }

      return adminToken;
   }
}
