package com.rackspace.papi.service.config.parser.inputstream;

import com.rackspace.papi.service.config.parser.common.AbstractConfigurationObjectParser;
import com.rackspace.papi.service.config.resource.ConfigurationResource;
import com.rackspace.papi.service.config.resource.ResourceResolutionException;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamConfigurationParser extends AbstractConfigurationObjectParser<InputStream> {

   public InputStreamConfigurationParser() {
      super(InputStream.class);
   }

   @Override
   public InputStream read(ConfigurationResource cr) {
      try {
         return cr.newInputStream();
      } catch (IOException ex) {
         throw new ResourceResolutionException("Unable to read configuration file: " + cr.name(), ex);
      }
   }
}
