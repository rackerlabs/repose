package com.rackspace.papi.mocks.auth.rs11.providers;

import com.rackspace.papi.mocks.auth.provider.UserDataPropertiesProviderImpl;
import com.rackspacecloud.docs.auth.api.v1.*;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Calendar;

public class AuthPropertiesProvider extends UserDataPropertiesProviderImpl implements AuthProvider {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthPropertiesProvider.class);
   private final ObjectFactory objectFactory;

   public AuthPropertiesProvider(String propertiesFile) throws DatatypeConfigurationException {
      super(propertiesFile);
      objectFactory = new ObjectFactory();
   }
   
   @Override
   public GroupsList getGroups(String userName) {
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
   
   @Override
   public Group getGroup(String userName, String groupId) {
      
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

   @Override
   public FullToken createToken(String userName, String id) {
      FullToken token = new FullToken();
      token.setId(id);
      token.setCreated(getCalendar(Calendar.DAY_OF_MONTH, -1));
      token.setExpires(getCalendar(Calendar.DAY_OF_MONTH, 10));
      token.setUserId(userName);
      token.setUserURL("/user/url");

      return token;
   }

   @Override
   public UnauthorizedFault createUnauthorized() {
      UnauthorizedFault fault = objectFactory.createUnauthorizedFault();
      fault.setCode(401);
      fault.setMessage("Username or api key is invalid");
      fault.setDetails("");
      //fault.setMessage("You are not authorized to access this resource.");
      //fault.setDetails("AuthErrorHandler");

      return fault;

   }

   @Override
   public ItemNotFoundFault createItemNotFound() {
      ItemNotFoundFault fault = objectFactory.createItemNotFoundFault();
      fault.setCode(404);
      fault.setMessage("Token not found.");
      fault.setDetails("");

      return fault;
   }
   
   @Override
   public boolean validateToken(String userName, String token, String accountType) {
      String validToken = userName.hashCode() + "";
      String providedToken = token.split(":")[0];

      return providedToken.equals(validToken);
   }
   
   @Override
   public String getUsername(String userId){
       
       return getProperties().getProperty("username."+userId);
   }
   
   
}
