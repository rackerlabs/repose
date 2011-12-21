package com.rackspace.papi.components.clientip;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.clientip.config.ClientIpIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class ClientIpIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ClientIpIdentityHandler> {

   public static final String DEFAULT_QUALITY = "0.1";
   private ClientIpIdentityConfig config;
   private String quality;

   public ClientIpIdentityHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ClientIpIdentityConfig.class, new ClientIpIdentityConfigurationListener());
         }
      };
   }

   private class ClientIpIdentityConfigurationListener implements UpdateListener<ClientIpIdentityConfig> {

      private String determineQuality() {
         String q = DEFAULT_QUALITY;

         if (config.getQuality() != null && !config.getQuality().trim().isEmpty()) {
            q = config.getQuality().trim();
         }

         return ";q=" + q;
      }

      @Override
      public void configurationUpdated(ClientIpIdentityConfig configurationObject) {
         config = configurationObject;
         quality = determineQuality();
      }
   }

   @Override
   protected ClientIpIdentityHandler buildHandler() {
      return new ClientIpIdentityHandler(config, quality);
   }
}
