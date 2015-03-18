/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.test.mocks.auth.provider;

import org.openrepose.commons.utils.test.mocks.DataProviderImpl;

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
