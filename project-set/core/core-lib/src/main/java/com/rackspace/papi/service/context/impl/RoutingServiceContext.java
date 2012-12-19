package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.routing.RoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("routingServiceContext")
public class RoutingServiceContext implements ServiceContext<RoutingService> {

   public static final String SERVICE_NAME = "powerapi:/services/routing";
   private final RoutingService service;
   private SystemModel config;
   private ConfigurationService configurationManager;
   private final PowerApiConfigListener configListener;
   private final ServiceRegistry registry;

   @Autowired
   public RoutingServiceContext(
           @Qualifier("routingService") RoutingService service,
           @Qualifier("serviceRegistry") ServiceRegistry registry,
           @Qualifier("configurationManager") ConfigurationService configurationManager) {
      this.service = service;
      configListener = new PowerApiConfigListener();
      this.registry = registry;
      this.configurationManager = configurationManager;
   }

   public void register() {
      if (registry != null) {
         registry.addService(this);
      }
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public RoutingService getService() {
      return service;
   }

   private class PowerApiConfigListener implements UpdateListener<SystemModel> {

      @Override
      public void configurationUpdated(SystemModel configurationObject) {
         config = configurationObject;
         service.setSystemModel(config);
      }
   }

   @Override
   public void contextInitialized(ServletContextEvent servletContextEvent) {
      configurationManager.subscribeTo("system-model.cfg.xml",configListener, SystemModel.class);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      if (configurationManager != null) {
         configurationManager.unsubscribeFrom("system-model.cfg.xml", configListener);
      }
   }
}
