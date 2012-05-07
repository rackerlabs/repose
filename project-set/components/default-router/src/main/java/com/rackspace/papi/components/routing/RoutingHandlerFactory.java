package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.SystemModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private final List<Port> ports;
   private SystemModel systemModel;

   public RoutingHandlerFactory(List<Port> ports) {
      this.ports = ports;
   }

   private class RoutingConfigurationListener implements UpdateListener<SystemModel> {

      @Override
      public void configurationUpdated(SystemModel configurationObject) {
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
            put(SystemModel.class, new RoutingConfigurationListener());
         }
      };
   }
}
