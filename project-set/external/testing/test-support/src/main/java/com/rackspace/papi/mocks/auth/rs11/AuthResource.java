package com.rackspace.papi.mocks.auth.rs11;

import com.rackspace.papi.mocks.auth.BaseAuthResource;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.sun.jersey.spi.resource.Singleton;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConfigurationException;

@Path("/v1.1")
@Singleton
public class AuthResource extends BaseAuthResource {
   private static final String DEFAULT_PROPS = "/auth1_1.properties";
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthResource.class);
   private final ObjectFactory objectFactory;

   public AuthResource() throws DatatypeConfigurationException, IOException {
      this(DEFAULT_PROPS);
   }
   
   public AuthResource(String propertiesFile) throws DatatypeConfigurationException, IOException {
      super(propertiesFile);
      objectFactory = new ObjectFactory();
   }

   @GET
   @Produces({"application/xml", "application/json"})
   public Response getToken(@HeaderParam("X-Auth-User") String userName, @HeaderParam("X-Auth-Key") String key) {
      if (!validateUser(userName)) {
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

      if (!validateUser(userName)) {
         return Response
                 .status(Status.UNAUTHORIZED)
                 .entity(objectFactory.createUnauthorized(createUnauthorized()))
                 .build();
      }
      if (!validateToken(userName, token, accountType)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(objectFactory.createItemNotFound(createItemNotFound()))
                 .build();
      }

      return Response.ok(objectFactory.createToken(createToken(userName, token))).build();
   }

   @GET
   @Path("/token/{token}")
   @Produces("application/json")
   public Response validateTokenJson(@PathParam("token") String token, @QueryParam("belongsTo") String userName, @QueryParam("type") String accountType, @Context UriInfo context) {

      if (!validateUser(userName)) {
         return Response
                 .status(Status.UNAUTHORIZED)
                 .entity(new UnauthorizedWrapper(createUnauthorized()))
                 .build();
      }

      if (!validateToken(userName, token, accountType)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(new ItemNotFoundWrapper(createItemNotFound()))
                 .build();
      }

      return Response.ok(new TokenWrapper(createToken(userName, token))).build();
   }

   @GET
   @Path("/users/{userId}/groups")
   @Produces({"application/xml"})
   public Response getGroups(@PathParam("userId") String userName) {
      if (!validateUser(userName)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(objectFactory.createItemNotFound(createItemNotFound()))
                 .build();
      }
      
      return Response.ok(objectFactory.createGroups(buildGroups(userName))).build();
   }
   
   @GET
   @Path("/users/{userId}/groups")
   @Produces({"application/json"})
   public Response getGroupsJson(@PathParam("userId") String userName) {
      if (!validateUser(userName)) {
         return Response
                 .status(Status.NOT_FOUND)
                 .entity(new ItemNotFoundWrapper(createItemNotFound()))
                 .build();
      }
      return Response.ok(new GroupsWrapper(buildGroups(userName))).build();
   }
   
   private GroupsList buildGroups(String userName) {
      GroupsList groupList = objectFactory.createGroupsList();
      
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
      String desc = getProperties().getProperty(baseGroup + ".desc");
      
      if (desc == null) {
         LOG.warn("Unable to find group details for role: " + baseGroup);
         desc = "Unknown";
      }
      
      Group group = objectFactory.createGroup();
      group.setId(groupId);
      group.setDescription(desc);
      
      return group;
   }
   
   // Wrapper classes so that Jackson can correctly wrap the elements in a 
   // root element
   
   @XmlRootElement(name="groups")
   private static class GroupsWrapper {
      private final ValuesWrapper values;
      public GroupsWrapper(GroupsList groups) {
         this.values = new ValuesWrapper(groups);
      }
      
      @XmlElement(name="groups")
      public ValuesWrapper getGroups() {
         return values;
      }
   }
   
   @XmlRootElement(name="values")
   private static class ValuesWrapper {
      private final List<Group> groups;
      public ValuesWrapper(GroupsList groups) {
         this.groups = groups.getGroup();
      }
      
      @XmlElement(name="values")
      public List<Group> getValues() {
         return groups;
      }
   }

   @XmlRootElement(name = "token")
   private static class TokenWrapper {

      private final FullToken token;

      public TokenWrapper(FullToken token) {
         this.token = token;
      }
      
      @XmlElement(name="token")
      public FullToken getToken() {
         return token;
      }
   }

   @XmlRootElement(name = "unauthorized")
   private static class UnauthorizedWrapper {

      private final UnauthorizedFault fault;

      public UnauthorizedWrapper(UnauthorizedFault fault) {
         this.fault = fault;
      }

      public UnauthorizedFault getUnathorized() {
         return fault;
      }
   }

   @XmlRootElement(name = "itemNotFound")
   private static class ItemNotFoundWrapper {

      private final ItemNotFoundFault fault;

      public ItemNotFoundWrapper(ItemNotFoundFault fault) {
         this.fault = fault;
      }

      public ItemNotFoundFault getItemNotFound() {
         return fault;
      }
   }

   private FullToken createToken(String userName, String id) {
      FullToken token = new FullToken();
      token.setId(id);
      token.setCreated(getCalendar(Calendar.DAY_OF_MONTH, -1));
      token.setExpires(getCalendar(Calendar.DAY_OF_MONTH, 10));
      token.setUserId(userName);
      token.setUserURL("/user/url");

      return token;
   }

   private UnauthorizedFault createUnauthorized() {
      UnauthorizedFault fault = objectFactory.createUnauthorizedFault();
      fault.setCode(401);
      fault.setMessage("Username or api key is invalid");
      fault.setDetails("");
      //fault.setMessage("You are not authorized to access this resource.");
      //fault.setDetails("AuthErrorHandler");

      return fault;

   }

   private ItemNotFoundFault createItemNotFound() {
      ItemNotFoundFault fault = objectFactory.createItemNotFoundFault();
      fault.setCode(404);
      fault.setMessage("Token not found.");
      fault.setDetails("");

      return fault;
   }

   private boolean validateToken(String userName, String token, String accountType) {
      String validToken = userName.hashCode() + "";
      String providedToken = token.split(":")[0];
      System.out.println("Token: [" + token + "] User Name: [" + userName + "] Account Type: [" + accountType + "] Valid Token: [" + validToken + "] Provided Token: [" + providedToken + "] isMatch: [" + providedToken.equals(validToken) + "]");

      return providedToken.equals(validToken);
   }
}
