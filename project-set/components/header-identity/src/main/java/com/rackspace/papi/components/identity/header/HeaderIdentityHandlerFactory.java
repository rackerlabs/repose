package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;


public class HeaderIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdentityHandler> {


   private HeaderIdentityConfig config;

   public HeaderIdentityHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(HeaderIdentityConfig.class, new HeaderIdentityConfigurationListener());
         }
      };
   }

   private class HeaderIdentityConfigurationListener implements UpdateListener<HeaderIdentityConfig> {

      @Override
      public void configurationUpdated(HeaderIdentityConfig configurationObject) {
         config = configurationObject;
      }
   }

   @Override
   protected HeaderIdentityHandler buildHandler() {
      return new HeaderIdentityHandler(config);
   }
}
