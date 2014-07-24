package com.rackspace.papi.service.config.parser.properties;

import com.rackspace.papi.service.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.core.service.config.resource.ConfigurationResource;
import org.openrepose.core.service.config.resource.ResourceResolutionException;

import java.io.IOException;
import java.util.Properties;

public class PropertiesFileConfigurationParser extends AbstractConfigurationObjectParser<Properties> {

   public PropertiesFileConfigurationParser() {
      super(Properties.class);
   }

   @Override
   public Properties read(ConfigurationResource cr) {
      Properties properties = new Properties();
      try {
         properties.load(cr.newInputStream());
      } catch (IOException ex) {
         throw new ResourceResolutionException("Unable to read configuration file: " + cr.name(), ex);
      }
      return properties;
   }
}
