package org.openrepose.commons.config.parser.inputstream;

import org.openrepose.commons.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ResourceResolutionException;

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
