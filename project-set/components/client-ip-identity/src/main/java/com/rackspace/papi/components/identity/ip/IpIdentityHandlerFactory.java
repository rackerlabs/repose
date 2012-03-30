package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class IpIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<IpIdentityHandler> {

   private IpIdentityConfig config;

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


      @Override
      public void configurationUpdated(IpIdentityConfig configurationObject) {
         config = configurationObject;
      }
   }

   @Override
   protected IpIdentityHandler buildHandler() {
      return new IpIdentityHandler(config);
   }
}
