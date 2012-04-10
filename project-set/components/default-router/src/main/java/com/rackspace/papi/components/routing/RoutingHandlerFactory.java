package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.PowerProxy;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private static final Logger LOG = LoggerFactory.getLogger(RoutingHandlerFactory.class);
   private final int httpPort;
   private final int httpsPort;
   private PowerProxy systemModel;

   public RoutingHandlerFactory(int httpPort, int httpsPort) {
      this.httpPort = httpPort;
      this.httpsPort = httpsPort;
   }

   private class RoutingConfigurationListener implements UpdateListener<PowerProxy> {

      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         systemModel = configurationObject;
      }
   }

   @Override
   protected RoutingTagger buildHandler() {
      return new RoutingTagger(new SystemModelInterrogator(systemModel, httpPort, httpsPort));
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(PowerProxy.class, new RoutingConfigurationListener());
         }
      };
   }
}
