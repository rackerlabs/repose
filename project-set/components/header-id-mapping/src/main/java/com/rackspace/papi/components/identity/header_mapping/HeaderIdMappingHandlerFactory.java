package com.rackspace.papi.components.identity.header_mapping;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.header_mapping.config.HeaderIdMappingConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;


public class HeaderIdMappingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdMappingHandler> {


   private HeaderIdMappingConfig config;

   public HeaderIdMappingHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(HeaderIdMappingConfig.class, new HeaderIdMappingConfigurationListener());
         }
      };
   }

   private class HeaderIdMappingConfigurationListener implements UpdateListener<HeaderIdMappingConfig> {

      @Override
      public void configurationUpdated(HeaderIdMappingConfig configurationObject) {
         config = configurationObject;
      }
   }

   @Override
   protected HeaderIdMappingHandler buildHandler() {
      return new HeaderIdMappingHandler(config);
   }
}
