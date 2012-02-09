package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class HeaderIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdentityHandler> {

   public static final String DEFAULT_QUALITY = "0.1";
   private HeaderIdentityConfig config;
   private String quality;

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

      private String determineQuality() {
         String q = DEFAULT_QUALITY;

         if (config.getQuality() != null && !config.getQuality().trim().isEmpty()) {
            q = config.getQuality().trim();
         }

         return ";q=" + q;
      }

      @Override
      public void configurationUpdated(HeaderIdentityConfig configurationObject) {
         config = configurationObject;
         quality = determineQuality();
      }
   }

   @Override
   protected HeaderIdentityHandler buildHandler() {
      return new HeaderIdentityHandler(config, quality);
   }
}
