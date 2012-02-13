package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class IpIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<IpIdentityHandler> {

   public static final String DEFAULT_QUALITY = "0.1";
   private IpIdentityConfig config;
   private String quality;

   public IpIdentityHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(IpIdentityConfig.class, new ClientIpIdentityConfigurationListener());
         }
      };
   }

   private class ClientIpIdentityConfigurationListener implements UpdateListener<IpIdentityConfig> {

      private String determineQuality() {
         String q = DEFAULT_QUALITY;

         if (config.getQuality() != null && !config.getQuality().trim().isEmpty()) {
            q = config.getQuality().trim();
         }

         return ";q=" + q;
      }

      @Override
      public void configurationUpdated(IpIdentityConfig configurationObject) {
         config = configurationObject;
         quality = determineQuality();
      }
   }

   @Override
   protected IpIdentityHandler buildHandler() {
      return new IpIdentityHandler(config, quality);
   }
}
