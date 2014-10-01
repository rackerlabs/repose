package com.rackspace.papi.mocks.auth.rs11;

import com.rackspace.papi.mocks.BaseResource;
import com.rackspace.papi.mocks.auth.rs11.providers.AuthPropertiesProvider;
import com.rackspace.papi.mocks.auth.rs11.providers.AuthProvider;
import com.rackspace.papi.mocks.auth.rs11.wrappers.JaxbElementWrapper;
import com.rackspace.papi.mocks.auth.rs11.wrappers.JsonElementWrapper;
import com.rackspace.papi.mocks.auth.rs11.wrappers.ResponseWrapper;
import com.sun.jersey.spi.resource.Singleton;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@Path("/v1.1")
@Singleton
public class AuthResource extends BaseResource {
   private static final String DEFAULT_PROPS = "/auth1_1.properties";

   public AuthResource() throws DatatypeConfigurationException, IOException {
      this(DEFAULT_PROPS);
   }
   
   public AuthResource(String propertiesFile) throws DatatypeConfigurationException, IOException {
      super(new AuthPropertiesProvider(propertiesFile));
   }

   @GET
   @Produces({"application/xml", "application/json"})
   public Response getToken(@HeaderParam("X-Auth-User") String userName, @HeaderParam("X-Auth-Key") String key) {
      AuthProvider p = getProvider();
      
      if (!p.validateUser(userName)) {
         return Response
                 .status(Status.UNAUTHORIZED)
                 .type(MediaType.APPLICATION_OCTET_STREAM)
                 .entity("Bad username or password")
                 .build();
      }
      
      CacheControl control = new CacheControl();
      control.setMaxAge(85961);
      return Response
              .noContent()
              .header("X-Auth-Token", userName.hashCode() + ":" + key.hashCode())
              .header("X-Storage-Url", "/storage/" + userName)
              .header("X-Storage-Token", userName.hashCode() + ":" + key.hashCode())
              .header("X-CDN-Management-Url", "/cdn/" + userName)
              .header("X-Server-Management-Url", "/server/" + userName)
              .cacheControl(control)
              .build();
   }

   @GET
   @Path("/token/{token}")
   @Produces("application/xml")
   public Response validateToken(@PathParam("token") String token, @QueryParam("belongsTo") String userName, @QueryParam("type") String accountType, @Context UriInfo context) throws DatatypeConfigurationException {
      AuthProvider p = getProvider();
      ResponseWrapper wrapper = new JaxbElementWrapper();

      if (!p.validateUser(userName)) {
         return Response
                 .status(Status.UNAUTHORIZED)
                 .entity(wrapper.wrapElement(p.createUnauthorized()))
                 .build();
      }
      if (!p.validateToken(userName, token, accountType)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(wrapper.wrapElement(p.createItemNotFound()))
                 .build();
      }

      return Response.ok(wrapper.wrapElement(p.createToken(userName, token))).build();
   }

   @GET
   @Path("/token/{token}")
   @Produces("application/json")
   public Response validateTokenJson(@PathParam("token") String token, @QueryParam("belongsTo") String userName, @QueryParam("type") String accountType, @Context UriInfo context) {
      AuthProvider p = getProvider();
      ResponseWrapper wrapper = new JsonElementWrapper();

      if (!p.validateUser(userName)) {
         return Response
                 .status(Status.UNAUTHORIZED)
                 .entity(wrapper.wrapElement(p.createUnauthorized()))
                 .build();
      }

      if (!p.validateToken(userName, token, accountType)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(wrapper.wrapElement(p.createItemNotFound()))
                 .build();
      }

      return Response.ok(wrapper.wrapElement(p.createToken(userName, token))).build();
   }

   @GET
   @Path("/users/{userId}/groups")
   @Produces({"application/xml"})
   public Response getGroups(@PathParam("userId") String userName) {
      AuthProvider p = getProvider();
      ResponseWrapper wrapper = new JaxbElementWrapper();

      if (!p.validateUser(userName)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(wrapper.wrapElement(p.createItemNotFound()))
                 .build();
      }
      
      return Response.ok(wrapper.wrapElement(p.getGroups(userName))).build();
   }
   
   @GET
   @Path("/users/{userId}/groups")
   @Produces({"application/json"})
   public Response getGroupsJson(@PathParam("userId") String userName) {
      AuthProvider p = getProvider();
      ResponseWrapper wrapper = new JsonElementWrapper();

      if (!p.validateUser(userName)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(wrapper.wrapElement(p.createItemNotFound()))
                 .build();
      }
      return Response.ok(wrapper.wrapElement(p.getGroups(userName))).build();
   }
   
   @GET
   @Path("/mosso/{userId}")
   public Response geMossoUserName(@PathParam("userId") String userId){
       AuthProvider p = getProvider();
       String location = "/v1.1/users/"+p.getUsername(userId);
       return Response.status(Status.MOVED_PERMANENTLY)
               .header("Location",location)
               .build();
   }
   
   @GET
   @Path("/users/{userId}")
   public Response getCloudUsername(@PathParam("userId") String userId){
       return geMossoUserName(userId);
   }
}
