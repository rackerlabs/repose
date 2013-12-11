package com.rackspace.papi.mocks.auth.provider;

import com.rackspace.papi.mocks.DataProviderImpl;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UserDataPropertiesProviderImpl extends DataProviderImpl implements UserDataProvider {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UserDataPropertiesProviderImpl.class);
   private String[] validUsers;
   private final Properties properties = new Properties();
   private boolean loaded = false;
   private final String propertiesFile;
   
   public UserDataPropertiesProviderImpl(String propertiesFile) throws DatatypeConfigurationException {
      this.propertiesFile = propertiesFile;
      loadProperties();
   }
   
   protected void loadProperties() {
      if (loaded) {
         return;
      }
      
      InputStream in = UserDataPropertiesProviderImpl.class.getResourceAsStream(propertiesFile);
      
      if (in != null) {
         try {
            properties.load(in);
            validUsers = properties.getProperty("validUsers").split(",");
            for (String user: validUsers) {
               LOG.info("Adding test user: " + user);
            }
         } catch (IOException ex) {
            LOG.error("Error loading auth properties: " + propertiesFile, ex);
         }
      } else {
         LOG.warn("Unable to find properties file: " + propertiesFile);
         validUsers = new String[0];
      }
      
      loaded = true;
      
   }
   
   @Override
   public int getUserId(String userName) {
      
      int i = 0;
      
      for (String user : validUsers) {
         if (user.equals(userName)) {
            return i;
         }
         
         i++;
      }
      return -1;
   }
   
   @Override
   public String getUserName(Integer id) {
      
      if (id == null || id < 0 || id >= validUsers.length) {
         return null;
      }
      
      return validUsers[id];
   }
   
   public Properties getProperties() {
      return properties;
   }

   @Override
   public String[] getValidUsers() {
      return validUsers;
   }
   
   @Override
   public boolean validateUser(String userName) {
      for (String user: validUsers) {
         if (user.equals(userName)) {
            return true;
         }
      }
      
      return false;
   }
   
}
