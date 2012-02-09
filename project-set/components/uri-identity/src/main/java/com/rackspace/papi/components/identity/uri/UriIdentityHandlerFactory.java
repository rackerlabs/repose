package com.rackspace.papi.components.identity.uri;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.uri.config.UriIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class UriIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriIdentityHandler> {

   public static final String DEFAULT_QUALITY = "0.5";
   private UriIdentityConfig config;
   private String quality;

   public UriIdentityHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(UriIdentityConfig.class, new UriIdentityConfigurationListener());
         }
      };
   }

   private class UriIdentityConfigurationListener implements UpdateListener<UriIdentityConfig> {

      private String determineQuality() {
         String q = DEFAULT_QUALITY;

         if (config.getQuality() != null && !config.getQuality().trim().isEmpty()) {
            q = config.getQuality().trim();
         }

         return ";q=" + q;
      }

      @Override
      public void configurationUpdated(UriIdentityConfig configurationObject) {
         config = configurationObject;
         quality = determineQuality();
      }
   }

   @Override
   protected UriIdentityHandler buildHandler() {
      return new UriIdentityHandler(config, quality);
   }
}
