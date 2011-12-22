package com.rackspace.papi.mocks.auth.osids;

import com.rackspace.papi.mocks.BaseResource;
import com.rackspace.papi.mocks.auth.osids.providers.KeystonePropertiesProvider;
import com.rackspace.papi.mocks.auth.osids.providers.KeystoneProvider;
import com.rackspace.papi.mocks.auth.osids.wrappers.JaxbElementWrapper;
import com.rackspace.papi.mocks.auth.osids.wrappers.ResponseWrapper;
import com.sun.jersey.spi.resource.Singleton;
import java.io.IOException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.DatatypeConfigurationException;
import org.openstack.docs.identity.api.v2.*;

@Path("/keystone")
@Singleton
public class KeystoneResource extends BaseResource {
   private static final String DEFAULT_PROPS = "/keystone.properties";
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(KeystoneResource.class);
   
   public KeystoneResource() throws DatatypeConfigurationException, IOException {
      this(DEFAULT_PROPS);
   }
   
   public KeystoneResource(String propertiesFile) throws DatatypeConfigurationException, IOException {
      super(new KeystonePropertiesProvider(propertiesFile));
   }
   
   @POST
   @Path("/tokens")
   @Consumes(MediaType.APPLICATION_XML)
   @Produces(MediaType.APPLICATION_XML)
   public Response getToken(AuthenticationRequest authRequest, @Context UriInfo context) throws DatatypeConfigurationException {
      KeystoneProvider p = getProvider();
      ResponseWrapper wrapper = new JaxbElementWrapper();
      
      CredentialType credentialType = authRequest.getCredential().getValue();
      if (!validateCredentials(credentialType)) {
         return Response
                 .status(Response.Status.UNAUTHORIZED)
                 .entity(wrapper.wrapElement(p.createUnauthorized("SecurityServiceFault: Unable to get customer auth by username")))
                 .build();
      }
      
      PasswordCredentialsRequiredUsername credentials = (PasswordCredentialsRequiredUsername)credentialType;
      Token token = p.createToken(credentialType);
      
      AuthenticateResponse response = p.newAuthenticateResponse();
      response.setToken(token);
      response.setServiceCatalog(p.getServiceCatalog(credentials.getUsername()));
      response.setUser(p.getUser(credentials.getUsername()));

      return Response.ok(wrapper.wrapElement(response)).build();
   }
   
   @GET
   @Path("/tokens/{token}")
   @Produces(MediaType.APPLICATION_XML)
   public Response validateToken(@PathParam("token") String userToken, @HeaderParam("X-Auth-Token") String authToken, @Context UriInfo context) {
      KeystoneProvider p = getProvider();
      ResponseWrapper wrapper = new JaxbElementWrapper();
      
      if (!p.isValidToken(userToken)) {
         
         return Response
                 .status(Response.Status.NOT_FOUND)
                 .entity(wrapper.wrapElement(p.createItemNotFound()))
                 .build();
      }
      
      if (!p.isValidToken(authToken)) {
         return Response
                 .status(Response.Status.UNAUTHORIZED)
                 .entity(wrapper.wrapElement(p.createUnauthorized("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")))
                 .build();
      }

      String userName = p.getUsernameFromToken(userToken);
      
      AuthenticateResponse response = p.newAuthenticateResponse();

      Token token = p.createToken(userToken);
      response.setToken(token);
      response.setUser(p.getUser(userName));

      return Response.ok(wrapper.wrapElement(response)).build();
   }
   
   @GET
   @Path("/users/{userId}/RAX-KSGRP")
   @Produces(MediaType.APPLICATION_XML)
   public Response getGroups(@PathParam("userId") String userId, @HeaderParam("X-Auth-Token") String authToken) {
      KeystoneProvider p = getProvider();
      ResponseWrapper wrapper = new JaxbElementWrapper();
      Integer id;
      
      try {
         id = Integer.valueOf(userId);
      } catch(NumberFormatException ex) {
         id = -1;
      }
      
      String userName = p.getUserName(id);
      
      if (!p.validateUser(userName)) {
         return Response
                 .status(Response.Status.NOT_FOUND)
                 .entity(wrapper.wrapElement(p.createItemNotFound()))
                 .build();
      }
      
      if (!p.isValidToken(authToken)) {
         return Response
                 .status(Response.Status.UNAUTHORIZED)
                 .entity(wrapper.wrapElement(p.createUnauthorized("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")))
                 .build();
      }
      
      return Response.ok(wrapper.wrapElement(p.getGroups(userName))).build();
   }
   
   protected boolean validateCredentials(CredentialType credentialType) {
      KeystoneProvider p = getProvider();
      PasswordCredentialsRequiredUsername credentials = (PasswordCredentialsRequiredUsername)credentialType;
      return p.validateUser(credentials.getUsername());
   }
   
}
