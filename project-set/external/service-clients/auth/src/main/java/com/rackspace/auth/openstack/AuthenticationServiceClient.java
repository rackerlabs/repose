package com.rackspace.auth.openstack;

import com.rackspace.auth.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import java.util.*;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

/**
 * @author fran
 */
public class AuthenticationServiceClient implements AuthenticationService {

   private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClient.class);
   private static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
   private static final String ACCEPT_HEADER = "Accept";
   private final String targetHostUri;
   private final ServiceClient serviceClient;
   private final ResponseUnmarshaller openStackCoreResponseUnmarshaller;
   private final ResponseUnmarshaller openStackGroupsResponseUnmarshaller;
   private AdminToken currentAdminToken;
   private final JAXBElement jaxbRequest;
   private final ObjectFactory objectFactory;

   public AuthenticationServiceClient(String targetHostUri, String username, String password,
           ResponseUnmarshaller openStackCoreResponseUnmarshaller,
           ResponseUnmarshaller openStackGroupsResponseUnmarshaller) {
      this.openStackCoreResponseUnmarshaller = openStackCoreResponseUnmarshaller;
      this.openStackGroupsResponseUnmarshaller = openStackGroupsResponseUnmarshaller;
      this.serviceClient = new ServiceClient();
      this.targetHostUri = targetHostUri;

      objectFactory = new ObjectFactory();
      PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
      credentials.setUsername(username);
      credentials.setPassword(password);

      JAXBElement jaxbCredentials = objectFactory.createPasswordCredentials(credentials);

      AuthenticationRequest request = new AuthenticationRequest();
      request.setCredential(jaxbCredentials);

      this.jaxbRequest = objectFactory.createAuth(request);
   }

   @Override
   public FullAuthInfo validateToken(String tenant, String userToken) {
      final Map<String, String> headers = new HashMap<String, String>();

      headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);


      FullAuthInfo fullAuthInfo = null;
      ServiceClientResponse<AuthenticateResponse> serviceResponse;

      if (!StringUtilities.isEmpty(tenant)) {

         AuthenticationRequest authRequest = new AuthenticationRequest();
         TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
         
         tokenForAuthenticationRequest.setId(userToken);
         authRequest.setTenantId(tenant);
         authRequest.setToken(tokenForAuthenticationRequest);
         final JAXBElement requestBody = objectFactory.createAuth(authRequest);
         serviceResponse = serviceClient.post(targetHostUri + "/tokens/", requestBody, MediaType.APPLICATION_XML_TYPE);
      } else {

         headers.put(AUTH_TOKEN_HEADER, getAdminToken());
         serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, headers);


      }

      switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
         case OK:
            final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
            fullAuthInfo = authenticateResponse.getServiceCatalog() == null ? new FullAuthInfo(new OpenStackToken(tenant, authenticateResponse), null) :
                    new FullAuthInfo(new OpenStackToken(tenant, authenticateResponse), getEndpointsForCache(authenticateResponse.getServiceCatalog()));
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

      return fullAuthInfo;
   }
   
   private List<CachedEndpoint> getEndpointsForCache(ServiceCatalog catalog){
      
      List<CachedEndpoint> cachedEndpoints = new LinkedList<CachedEndpoint>();
      
      for(ServiceForCatalog service: catalog.getService()){
         for(EndpointForService endpoint: service.getEndpoint()){
            cachedEndpoints.add(new CachedEndpoint(endpoint.getPublicURL(), endpoint.getRegion(), service.getName(), service.getType()));
         }
      }
      
      return cachedEndpoints;
   }

   @Override
   public List<Endpoint> getEndpointsForToken(String userToken) {
      final Map<String, String> headers = new HashMap<String, String>();

      headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
      headers.put(AUTH_TOKEN_HEADER, getAdminToken());

      final ServiceClientResponse<EndpointList> endpointListResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken + "/endpoints", headers);
      List<Endpoint> endpointList = new ArrayList<Endpoint>();

      switch (HttpStatusCode.fromInt(endpointListResponse.getStatusCode())) {
         case OK:
            final EndpointList unmarshalledEndpoints = openStackCoreResponseUnmarshaller.unmarshall(endpointListResponse.getData(), EndpointList.class);

            if (unmarshalledEndpoints != null) {
               endpointList = unmarshalledEndpoints.getEndpoint();
            }

            break;

         default:
            LOG.warn("Unable to get endpoints for token. Status code: " + endpointListResponse.getStatusCode());
            break;
      }

      return endpointList;
   }
   
   @Override
   public AuthGroups getGroups(String userId) {
      final Map<String, String> headers = new HashMap<String, String>();

      headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
      headers.put(AUTH_TOKEN_HEADER, getAdminToken());

      final ServiceClientResponse<Groups> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", headers);
      final List<AuthGroup> authGroupList = new ArrayList<AuthGroup>();
      AuthGroups authGroups = null;

      switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
         case OK:
            Groups groups = openStackGroupsResponseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);
            for (Group group : groups.getGroup()) {
               final AuthGroup authGroup = new OpenStackGroup(group);
               authGroupList.add(authGroup);
            }
            authGroups = new AuthGroups(authGroupList);
            break;

         default:
            LOG.warn("Unable to get groups for user id: " + userId + " Status code: " + serviceResponse.getStatusCode());
            break;

      }

      return authGroups;
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
