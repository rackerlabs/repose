package com.rackspace.papi.components.clientuser;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.clientuser.config.ClientUserIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class ClientUserIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ClientUserIdentityHandler> {

   public static final String DEFAULT_QUALITY = "0.5";
   private ClientUserIdentityConfig config;
   private String quality;

   public ClientUserIdentityHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ClientUserIdentityConfig.class, new ClientUserIdentityConfigurationListener());
         }
      };
   }

   private class ClientUserIdentityConfigurationListener implements UpdateListener<ClientUserIdentityConfig> {

      private String determineQuality() {
         String q = DEFAULT_QUALITY;

         if (config.getQuality() != null && !config.getQuality().trim().isEmpty()) {
            q = config.getQuality().trim();
         }

         return ";q=" + q;
      }

      @Override
      public void configurationUpdated(ClientUserIdentityConfig configurationObject) {
         config = configurationObject;
         quality = determineQuality();
      }
   }

   @Override
   protected ClientUserIdentityHandler buildHandler() {
      return new ClientUserIdentityHandler(config, quality);
   }
}
