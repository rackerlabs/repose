package com.rackspace.papi.mocks.auth.osids;



import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.mocks.auth.BaseAuthResource;
import com.sun.jersey.spi.resource.Singleton;
import java.io.IOException;
import java.util.Calendar;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.DatatypeConfigurationException;
import org.openstack.docs.identity.api.v2.*;

@Path("/keystone")
@Singleton
public class KeystoneResource extends BaseAuthResource {
   private static final String DEFAULT_PROPS = "/keystone.properties";
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(KeystoneResource.class);
   private final ObjectFactory objectFactory;
   private final com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory groupsObjectFactory;
   
   public KeystoneResource() throws DatatypeConfigurationException, IOException {
      this(DEFAULT_PROPS);
   }
   
   public KeystoneResource(String propertiesFile) throws DatatypeConfigurationException, IOException {
      super(propertiesFile);
      objectFactory = new ObjectFactory();
      groupsObjectFactory = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();
   }
   
   @POST
   @Path("/tokens")
   @Consumes("application/xml")
   @Produces("application/xml")
   public Response getToken(AuthenticationRequest authRequest, @Context UriInfo context) throws DatatypeConfigurationException {
      
      CredentialType credentialType = authRequest.getCredential().getValue();
      if (!validateCredentials(credentialType)) {
         return Response
                 .status(Response.Status.UNAUTHORIZED)
                 .entity(objectFactory.createUnauthorized(createUnauthorized("SecurityServiceFault: Unable to get customer auth by username")))
                 .build();
      }
      
      PasswordCredentialsRequiredUsername credentials = (PasswordCredentialsRequiredUsername)credentialType;
      Token token = createToken(credentialType);
      
      AuthenticateResponse response = objectFactory.createAuthenticateResponse();
      response.setToken(token);
      response.setServiceCatalog(objectFactory.createServiceCatalog());
      response.setUser(getUser(credentials.getUsername()));

      return Response.ok(objectFactory.createAccess(response)).build();
   }
   
   @GET
   @Path("/tokens/{token}")
   @Produces("application/xml")
   public Response validateToken(@PathParam("token") String userToken, @HeaderParam("X-Auth-Token") String authToken, @Context UriInfo context) {
      
      if (!isValidToken(userToken)) {
         
         return Response
                 .status(Response.Status.NOT_FOUND)
                 .entity(objectFactory.createItemNotFound(createItemNotFound()))
                 .build();
      }
      
      if (!isValidToken(authToken)) {
         return Response
                 .status(Response.Status.UNAUTHORIZED)
                 .entity(objectFactory.createUnauthorized(createUnauthorized("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")))
                 .build();
      }

      String userName = getUsernameFromToken(userToken);
      
      AuthenticateResponse response = objectFactory.createAuthenticateResponse();

      Token token = createToken(userToken);
      response.setToken(token);
      //response.setServiceCatalog(objectFactory.createServiceCatalog());
      response.setUser(getUser(userName));

      return Response.ok(objectFactory.createAccess(response)).build();
   }
   
   @GET
   @Path("/users/{userId}/RAX-KSGRP")
   @Produces("application/xml")
   public Response getGroups(@PathParam("userId") Integer userId, @HeaderParam("X-Auth-Token") String authToken) {
      String userName = getUserName(userId);
      
      if (!validateUser(userName)) {
         return Response
                 .status(Response.Status.NOT_FOUND)
                 .entity(objectFactory.createItemNotFound(createItemNotFound()))
                 .build();
      }
      
      if (!isValidToken(authToken)) {
         return Response
                 .status(Response.Status.UNAUTHORIZED)
                 .entity(objectFactory.createUnauthorized(createUnauthorized("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")))
                 .build();
      }
      
      return Response.ok(groupsObjectFactory.createGroups(getGroups(userName))).build();
   }
   
   private String getUsernameFromToken(String token) {
      String userHash = token.split(":")[0];
      
      for (String user: getValidUsers()) {
         if (String.valueOf(user.hashCode()).equals(userHash)) {
            return user;
         }
      }
      
      return "";
   }
   
   private boolean isValidToken(String token) {
      String userHash = token != null? token.split(":")[0]: "";
      
      for (String user: getValidUsers()) {
         if (String.valueOf(user.hashCode()).equals(userHash)) {
            return true;
         }
      }
      
      return false;
   }
   
   private UserForAuthenticateResponse getUser(String userName) {
      UserForAuthenticateResponse user = objectFactory.createUserForAuthenticateResponse();
      
      user.setId(String.valueOf(getUserId(userName)));
      user.setName(userName);
      user.setRoles(getRoles(userName));

      return user;
   }
   
   private RoleList getRoles(String userName) {
      RoleList roleList = objectFactory.createRoleList();
      
      String roles = getProperties().getProperty("roles." + userName);
      
      if (roles == null) {
         LOG.warn("No roles defined for user: " + userName);
         return roleList;
      }
      
      String[] roleNames = roles.split(",");
      
      for (String roleId: roleNames) {
         Role role = getRole(userName, roleId);
         if (role != null) {
            roleList.getRole().add(role);
         }
      }
      
      return roleList;
   }
   
   private Role getRole(String userName, String roleId) {
      
      String baseRole = "role." + userName + "." + roleId;
      
      String name = getProperties().getProperty(baseRole + ".name");
      String desc = getProperties().getProperty(baseRole + ".desc", "");
      
      if (name == null) {
         LOG.warn("Unable to find role details for role: " + baseRole);
         name = roleId;
      }
      
      LOG.info("Adding role: " + roleId + " for user: " + userName);
      Role role = objectFactory.createRole();
      role.setId(roleId);
      role.setName(name);
      role.setDescription(desc);
      //role.setServiceId("serviceId");
      //role.setTenantId("tenantId");

      return role;
   }
   
   private Groups getGroups(String userName) {
      Groups groupList = groupsObjectFactory.createGroups();
      
      String groups = getProperties().getProperty("groups." + userName);
      
      if (groups == null) {
         LOG.warn("No groups defined for user: " + userName);
         return groupList;
      }
      
      String[] groupNames = groups.split(",");
      
      for (String groupId: groupNames) {
         Group group = getGroup(userName, groupId);
         if (group != null) {
            groupList.getGroup().add(group);
         }
      }
      
      return groupList;
   }
   
   private Group getGroup(String userName, String groupId) {
      
      String baseGroup = "group." + userName + "." + groupId;
      
      String name = getProperties().getProperty(baseGroup + ".name");
      String desc = getProperties().getProperty(baseGroup + ".desc", "");
      
      if (name == null) {
         LOG.warn("Unable to find group details for group: " + baseGroup);
         name = groupId;
      }
      
      LOG.info("Adding group: " + groupId + " for user: " + userName);
      Group group = groupsObjectFactory.createGroup();
      group.setId(groupId);
      group.setName(name);
      group.setDescription(desc);

      return group;
   }
   
   private TenantForAuthenticateResponse createTenant() {
      TenantForAuthenticateResponse tenant = objectFactory.createTenantForAuthenticateResponse();
      tenant.setId("tenantId");
      tenant.setName("tenantName");

      return tenant;
   }
   
   private Token createToken(String tokenId) {
      Token token = objectFactory.createToken();
      token.setId(tokenId);
      token.setExpires(getCalendar(Calendar.DAY_OF_MONTH, 30));
      //token.setTenant(createTenant());

      return token;
   }

   private Token createToken(CredentialType credentialType) {
      PasswordCredentialsRequiredUsername credentials = (PasswordCredentialsRequiredUsername)credentialType;
      
      return createToken(credentials.getUsername().hashCode() + ":" + credentials.getPassword().hashCode());
   }

   protected boolean validateCredentials(CredentialType credentialType) {
      PasswordCredentialsRequiredUsername credentials = (PasswordCredentialsRequiredUsername)credentialType;
      return validateUser(credentials.getUsername());
   }
   

   private ItemNotFoundFault createItemNotFound() {
      ItemNotFoundFault fault = objectFactory.createItemNotFoundFault();
      fault.setCode(404);
      fault.setMessage("Token not found.");
      fault.setDetails("");

      return fault;
   }

   private UnauthorizedFault createUnauthorized(String message) {
      UnauthorizedFault fault = objectFactory.createUnauthorizedFault();
      fault.setCode(401);
      fault.setMessage(message);
      fault.setDetails("");
      //fault.setMessage("You are not authorized to access this resource.");
      //fault.setDetails("AuthErrorHandler");

      return fault;

   }

   /*
   @XmlRootElement(name="access")
   private static class AuthenticateResponseWrapper {
      private final AuthenticateResponse access;
      public AuthenticateResponseWrapper(AuthenticateResponse access) {
         this.access = access;
      }
      
      public AuthenticateResponse getAccess() {
         return access;
      }
   }
   * 
   */
}
