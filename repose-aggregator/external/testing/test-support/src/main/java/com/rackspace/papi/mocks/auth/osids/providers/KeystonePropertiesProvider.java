package com.rackspace.papi.mocks.auth.osids.providers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.mocks.auth.provider.UserDataPropertiesProviderImpl;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Calendar;

public class KeystonePropertiesProvider extends UserDataPropertiesProviderImpl implements KeystoneProvider {

   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(KeystonePropertiesProvider.class);
   private final ObjectFactory objectFactory;
   private final com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory groupsObjectFactory;

   public KeystonePropertiesProvider(String propertiesFile) throws DatatypeConfigurationException {
      super(propertiesFile);
      objectFactory = new ObjectFactory();
      groupsObjectFactory = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();
   }

   @Override
   public String getUsernameFromToken(String token) {
      String userHash = token.split(":")[0];

      for (String user : getValidUsers()) {
         if (String.valueOf(user.hashCode()).equals(userHash)) {
            return user;
         }
      }

      return "";
   }

   @Override
   public boolean isValidToken(String token) {
      String userHash = token != null ? token.split(":")[0] : "";

      for (String user : getValidUsers()) {
         if (String.valueOf(user.hashCode()).equals(userHash)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public UserForAuthenticateResponse getUser(String userName) {
      UserForAuthenticateResponse user = objectFactory.createUserForAuthenticateResponse();

      user.setId(String.valueOf(getUserId(userName)));
      user.setName(userName);
      user.setRoles(getRoles(userName));

      return user;
   }

   @Override
   public ServiceCatalog getServiceCatalog(String userName) {
      return objectFactory.createServiceCatalog();
   }

   @Override
   public RoleList getRoles(String userName) {
      RoleList roleList = objectFactory.createRoleList();

      String roles = getProperties().getProperty("roles." + userName);

      if (roles == null) {
         LOG.warn("No roles defined for user: " + userName);
         return roleList;
      }

      String[] roleNames = roles.split(",");

      for (String roleId : roleNames) {
         Role role = getRole(userName, roleId);
         if (role != null) {
            roleList.getRole().add(role);
         }
      }

      return roleList;
   }

   @Override
   public Role getRole(String userName, String roleId) {

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

   @Override
   public Groups getGroups(String userName) {
      Groups groupList = groupsObjectFactory.createGroups();

      String groups = getProperties().getProperty("groups." + userName);

      if (groups == null) {
         LOG.warn("No groups defined for user: " + userName);
         return groupList;
      }

      String[] groupNames = groups.split(",");

      for (String groupId : groupNames) {
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

   @Override
   public TenantForAuthenticateResponse createTenant(String userName) {
      TenantForAuthenticateResponse tenant = objectFactory.createTenantForAuthenticateResponse();
      tenant.setId(userName);
      tenant.setName(userName);

      return tenant;
   }

   @Override
   public Token createToken(String tokenId) {
      Token token = objectFactory.createToken();
      token.setId(tokenId);
      token.setExpires(getCalendar(Calendar.DAY_OF_MONTH, 1));
      if (!getUsernameFromToken(tokenId).equalsIgnoreCase("notenant")) {
         token.setTenant(createTenant(getUsernameFromToken(tokenId)));
      }

      return token;
   }

   @Override
   public Token createToken(CredentialType credentialType) {
      PasswordCredentialsRequiredUsername credentials = (PasswordCredentialsRequiredUsername) credentialType;

      return createToken(credentials.getUsername().hashCode() + ":" + credentials.getPassword().hashCode());
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
   public UnauthorizedFault createUnauthorized(String message) {
      UnauthorizedFault fault = objectFactory.createUnauthorizedFault();
      fault.setCode(401);
      fault.setMessage(message);
      fault.setDetails("");
      //fault.setMessage("You are not authorized to access this resource.");
      //fault.setDetails("AuthErrorHandler");

      return fault;

   }

   @Override
   public AuthenticateResponse newAuthenticateResponse() {
      return objectFactory.createAuthenticateResponse();
   }

   @Override
   public EndpointList getEndpoints(String token) {
      EndpointList el = new EndpointList();
      String username = this.getUsernameFromToken(token);
      Endpoint endpoint;
      VersionForService version;
      String[] endpointIds = getProperties().getProperty("endpoints." + username).split(",");

      for (String eId : endpointIds) {
         endpoint = new Endpoint();
         endpoint.setId(Integer.parseInt(eId));
         endpoint.setType(getProperties().getProperty("type." + eId));
         endpoint.setName(getProperties().getProperty("name." + eId));
         endpoint.setPublicURL(getProperties().getProperty("publicurl." + eId) + "/" + username);
         endpoint.setInternalURL(getProperties().getProperty("internalurl." + eId) + "/" + username);
         endpoint.setAdminURL(getProperties().getProperty("adminurl." + eId));
         endpoint.setTenantId(username);
         endpoint.setRegion(getProperties().getProperty("region." + eId));
         String v = getProperties().getProperty("versions." + eId);
         if (v != null) {
            version = new VersionForService();
            version.setId(v);
            version.setInfo(getProperties().getProperty("version." + eId + "." + v + ".info"));
            version.setList(getProperties().getProperty("version." + eId + "." + v + ".list"));
            endpoint.setVersion(version);
         }

         el.getEndpoint().add(endpoint);

      }



      return el;
   }
}
