package com.rackspace.papi.components.identity.header_mapping;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.header_mapping.config.HeaderIdMappingConfig;
import com.rackspace.papi.components.identity.header_mapping.config.HttpHeader;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HeaderIdMappingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdMappingHandler> {


   private HeaderIdMappingConfig config;
   private List<HttpHeader> sourceHeaders;

   public HeaderIdMappingHandlerFactory() {
       sourceHeaders = new ArrayList<HttpHeader>();
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
          
          sourceHeaders = configurationObject.getSourceHeaders().getHeader();
      }
   }

   @Override
   protected HeaderIdMappingHandler buildHandler() {
      return new HeaderIdMappingHandler(sourceHeaders);
   }
}
