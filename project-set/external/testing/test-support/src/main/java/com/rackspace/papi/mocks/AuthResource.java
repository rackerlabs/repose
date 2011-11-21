package com.rackspace.papi.mocks;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.ItemNotFoundFault;
import com.rackspacecloud.docs.auth.api.v1.ObjectFactory;
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault;
import com.sun.jersey.spi.resource.Singleton;
import java.util.Calendar;
import javax.ws.rs.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

@Path("/v1.1")
@Singleton
public class AuthResource {

   private final ObjectFactory objectFactory;
   private final DatatypeFactory dataTypeFactory;
   private final String[] validUsers = {"cmarin1", "usertest1", "usertest2", "usertest3", "usertest4"};

   public AuthResource() throws DatatypeConfigurationException {
      objectFactory = new ObjectFactory();
      dataTypeFactory = DatatypeFactory.newInstance();
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

   // Wrapper classes so that Jackson can correctly wrap the elements in a 
   // root element
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

   private XMLGregorianCalendar getCalendar() {
      return getCalendar(Calendar.DAY_OF_YEAR, 0);
   }

   private XMLGregorianCalendar getCalendar(int field, int value) {
      Calendar calendar = Calendar.getInstance();
      if (value != 0) {
         calendar.setLenient(true);
         calendar.add(field, value);
      }
      int year = calendar.get(Calendar.YEAR);
      int month = calendar.get(Calendar.MONTH) + 1;
      int day = calendar.get(Calendar.DAY_OF_MONTH);
      int hour = calendar.get(Calendar.HOUR_OF_DAY);
      int min = calendar.get(Calendar.MINUTE);
      int sec = calendar.get(Calendar.SECOND);
      int milli = calendar.get(Calendar.MILLISECOND);
      int tz = calendar.get(Calendar.ZONE_OFFSET) / 60000;

      return dataTypeFactory.newXMLGregorianCalendar(year, month, day, hour, min, sec, milli, tz);
   }

   private FullToken createToken(String userName, String id) {
      FullToken token = new FullToken();
      token.setId(id);
      token.setCreated(getCalendar(Calendar.DAY_OF_MONTH, -1));
      token.setExpires(getCalendar(Calendar.DAY_OF_MONTH, 30));
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

   private boolean validateUser(String userName) {
      for (String user : validUsers) {
         if (user.equals(userName)) {
            return true;
         }
      }

      return false;
   }

   private boolean validateToken(String userName, String token, String accountType) {
      String validToken = userName.hashCode() + "";
      String providedToken = token.split(":")[0];
      System.out.println("Token: [" + token + "] User Name: [" + userName + "] Account Type: [" + accountType + "] Valid Token: [" + validToken + "] Provided Token: [" + providedToken + "] isMatch: [" + providedToken.equals(validToken) + "]");

      return providedToken.equals(validToken);
   }
}
