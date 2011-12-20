package com.rackspace.papi.components.clientip;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.clientip.config.ClientIpIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class ClientIpIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ClientIpIdentityHandler> {

   private ClientIpIdentityConfig config;
   
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
      @Override
      public void configurationUpdated(ClientIpIdentityConfig configurationObject) {
         config = configurationObject;
      }
   }
   
   @Override
   protected ClientIpIdentityHandler buildHandler() {
      return new ClientIpIdentityHandler(config);
   }
   
}
