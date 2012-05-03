package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.PowerProxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private final List<Port> ports;
   private PowerProxy systemModel;

   public RoutingHandlerFactory(List<Port> ports) {
      this.ports = ports;
   }

   private class RoutingConfigurationListener implements UpdateListener<PowerProxy> {

      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         systemModel = configurationObject;
      }
   }

   @Override
   protected RoutingTagger buildHandler() {
      return new RoutingTagger(new SystemModelInterrogator(systemModel, ports));
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
