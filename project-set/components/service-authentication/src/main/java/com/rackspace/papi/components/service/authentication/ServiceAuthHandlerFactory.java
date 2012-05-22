package com.rackspace.papi.components.service.authentication;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.HashMap;
import java.util.Map;

public class ServiceAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ServiceAuthHandler> {

   private ServiceAuthenticationConfig config;

   public ServiceAuthHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ServiceAuthenticationConfig.class, new ClientIpIdentityConfigurationListener());
         }
      };
   }

   private class ClientIpIdentityConfigurationListener implements UpdateListener<ServiceAuthenticationConfig> {


      @Override
      public void configurationUpdated(ServiceAuthenticationConfig configurationObject) {
         config = configurationObject;
      }
   }

   @Override
   protected ServiceAuthHandler buildHandler() {
      return new ServiceAuthHandler(config);
   }
}
