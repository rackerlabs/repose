package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.model.ServiceDomain;
import java.util.List;
import com.rackspace.papi.domain.Port;


public class DestinationRouterHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private DestinationRouterConfiguration contextRouterConfiguration;
   private final List<Port> ports;
   private ServiceDomain localDomain;
   private DomainNode localHost;
   
    public DestinationRouterHandlerFactory(List<Port> ports) {
        this.ports = ports;
    }
   
   

   private class RoutingConfigurationListener implements UpdateListener<DestinationRouterConfiguration> {

      @Override
      public void configurationUpdated(DestinationRouterConfiguration configurationObject) {
         contextRouterConfiguration = configurationObject;
      }
   }
   
   private class SystemModelConfigurationListener implements UpdateListener<PowerProxy> {

      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         SystemModelInterrogator interrogator = new SystemModelInterrogator(configurationObject, ports);
         localDomain = interrogator.getLocalServiceDomain();
         localHost = interrogator.getLocalHost();
      }
   }

   @Override
   protected RoutingTagger buildHandler() {
      return new RoutingTagger(contextRouterConfiguration.getTarget());
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
      updateListeners.put(DestinationRouterConfiguration.class, new RoutingConfigurationListener());
      updateListeners.put(PowerProxy.class, new SystemModelConfigurationListener());
      return updateListeners;
   }
}
