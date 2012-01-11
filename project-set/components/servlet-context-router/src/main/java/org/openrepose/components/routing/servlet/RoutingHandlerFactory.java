package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.PowerProxy;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.routing.servlet.config.ServletContextRouterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private static final Logger LOG = LoggerFactory.getLogger(RoutingHandlerFactory.class);
   private ServletContextRouterConfiguration contextRouterConfiguration;

   public RoutingHandlerFactory() {
   }

   private class RoutingConfigurationListener implements UpdateListener<ServletContextRouterConfiguration> {

      @Override
      public void configurationUpdated(ServletContextRouterConfiguration configurationObject) {
         contextRouterConfiguration = configurationObject;
      }
   }

   @Override
   protected RoutingTagger buildHandler() {
      return new RoutingTagger(contextRouterConfiguration.getContextPath());
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
