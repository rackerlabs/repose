package com.rackspace.papi.service.context;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.service.routing.robin.RoundRobinRoutingService;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class RoutingServiceContext implements ServiceContext<RoutingService> {
   public static final String SERVICE_NAME = "powerapi:/services/routing";
   private RoutingService service;
   private PowerProxy config;
   private ServletContext servletContext;
   private ConfigurationService configurationManager;
   private final PowerApiConfigListener configListener;
   
   public RoutingServiceContext() {
      configListener = new PowerApiConfigListener();
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public RoutingService getService() {
      return service;
   }
   
   private class PowerApiConfigListener implements UpdateListener<PowerProxy> {
      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         config = configurationObject;
         service = new RoundRobinRoutingService(config);
      }
      
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent) {
      servletContext = servletContextEvent.getServletContext();
      configurationManager = ServletContextHelper.getPowerApiContext(servletContext).configurationService();

      configurationManager.subscribeTo("power-proxy.cfg.xml", configListener, PowerProxy.class);
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      if (configurationManager != null) {
         configurationManager.unsubscribeFrom("power-proxy.cfg.xml", configListener);
      }
   }
   
}
