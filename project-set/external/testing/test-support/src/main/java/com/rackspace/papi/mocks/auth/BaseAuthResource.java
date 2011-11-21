package com.rackspace.papi.mocks.auth;

import com.rackspace.papi.mocks.BaseResource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.xml.datatype.DatatypeConfigurationException;

public class BaseAuthResource extends BaseResource {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BaseAuthResource.class);
   private final Properties properties = new Properties();
   private final String[] validUsers;
   
   public BaseAuthResource(String propertiesFile) throws DatatypeConfigurationException, IOException {
      super();
      
      InputStream in = BaseAuthResource.class.getResourceAsStream(propertiesFile);
      
      if (in != null) {
         properties.load(in);
         validUsers = properties.getProperty("validUsers").split(",");
         for (String user: validUsers) {
            LOG.info("Adding test user: " + user);
         }
      } else {
         LOG.warn("Unable to find properties file: " + propertiesFile);
         validUsers = new String[0];
      }
   }
   
   public Properties getProperties() {
      return properties;
   }

   protected String[] getValidUsers() {
      return validUsers;
   }
   
   protected boolean validateUser(String userName) {
      for (String user : validUsers) {
         if (user.equals(userName)) {
            return true;
         }
      }

      return false;
   }
   
   protected int getUserId(String userName) {
      int i = 0;
      
      for (String user : validUsers) {
         if (user.equals(userName)) {
            return i;
         }
         
         i++;
      }
      return -1;
   }

}
