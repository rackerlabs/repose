package com.rackspace.papi.components.reverseproxy.basicauth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class ReverseProxyBasicAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ReverseProxyBasicAuthHandler> {

   private ReverseProxyBasicAuthConfig config;

   public ReverseProxyBasicAuthHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(ReverseProxyBasicAuthConfig.class, new ClientIpIdentityConfigurationListener());
         }
      };
   }

   private class ClientIpIdentityConfigurationListener implements UpdateListener<ReverseProxyBasicAuthConfig> {


      @Override
      public void configurationUpdated(ReverseProxyBasicAuthConfig configurationObject) {
         config = configurationObject;
      }
   }

   @Override
   protected ReverseProxyBasicAuthHandler buildHandler() {
      return new ReverseProxyBasicAuthHandler(config);
   }
}
