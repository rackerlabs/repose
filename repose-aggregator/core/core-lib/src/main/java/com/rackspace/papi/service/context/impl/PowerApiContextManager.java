package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextAware;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.banner.PapiBanner;
import com.rackspace.papi.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Map;

public class PowerApiContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(PowerApiContextManager.class);
   private AbstractApplicationContext applicationContext;
   private boolean contextInitialized = false;

   public PowerApiContextManager() {
   }

   private void intializeServices(ServletContextEvent sce) {
      ServletContextHelper helper = ServletContextHelper.getInstance(sce.getServletContext());
      ContextAdapter ca = helper.getPowerApiContext();

      PapiBanner.print(LOG);
      // TODO:Refactor - This service should be bound to a fitler-chain specific JNDI context
      ca.getContext(DistributedDatastoreServiceContext.class).contextInitialized(sce);
      ca.getContext(HttpConnectionPoolServiceContext.class).contextInitialized(sce);
      ca.getContext(AkkaServiceClientContext.class).contextInitialized(sce);

      Map<String, ServletContextAware> contextAwareBeans = applicationContext.getBeansOfType(ServletContextAware.class);

      for (ServletContextAware bean : contextAwareBeans.values()) {
         bean.contextInitialized(sce);
      }

   }

   private boolean isManagementServerEnabled() {
      return System.getProperty(InitParameter.MANAGEMENT_PORT.getParameterName()) != null;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext servletContext = sce.getServletContext();

      // Most bootstrap steps require or will try to load some kind of
      // configuration so we need to set our naming context in the servlet context
      // first before anything else
      ServletContextHelper.configureInstance(
              servletContext,
              applicationContext);

       //TODO: does stuff use this?
      servletContext.setAttribute("powerApiContextManager", this);
      contextInitialized = true;
   }

   public boolean isContextInitialized() {
      return contextInitialized;
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      contextInitialized = false;

      Map<String, ServletContextAware> contextAwareBeans = applicationContext.getBeansOfType(ServletContextAware.class);

      for (ServletContextAware bean : contextAwareBeans.values()) {
         bean.contextDestroyed(sce);
      }

      ServiceRegistry registry = applicationContext.getBean("serviceRegistry", ServiceRegistry.class);
      for (ServiceContext ctx : registry.getServices()) {
         ctx.contextDestroyed(sce);
      }

      LOG.info("Shutting down Spring application context");
      applicationContext.close();

   }
}
