package com.rackspace.cloud.valve.controller.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.impl.ConfigurationServiceContext;
import com.rackspace.papi.service.context.impl.EventManagerServiceContext;
import com.rackspace.papi.service.context.impl.LoggingServiceContext;
import com.rackspace.papi.service.context.impl.ReportingServiceContext;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;
import com.rackspace.papi.spring.SpringConfiguration;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ReposeValveControllerContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(ReposeValveControllerContextManager.class);
   private AnnotationConfigApplicationContext applicationContext;

   public ReposeValveControllerContextManager() {
   }

   private void intializeServices(ServletContextEvent sce) {
      final ServletContext servletContext = sce.getServletContext();
      ServletContextHelper helper = ServletContextHelper.getInstance(sce.getServletContext());
      ContextAdapter ca = helper.getPowerApiContext();

      ca.getContext(ThreadingServiceContext.class).contextInitialized(sce);
      ca.getContext(EventManagerServiceContext.class).contextInitialized(sce);
      ca.getContext(ConfigurationServiceContext.class).contextInitialized(sce);
      ca.getContext(LoggingServiceContext.class).contextInitialized(sce);
      ca.getContext(ReportingServiceContext.class).contextInitialized(sce);
      ca.getContext(ReposeValveControllerContext.class).contextInitialized(sce);

      servletContext.setAttribute("reposeValveControllerContextManager", this);
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {

      final ServletContext servletContext = sce.getServletContext();

      applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);

      ServletContextHelper.configureInstance(
              servletContext,
              applicationContext);
      intializeServices(sce);

   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      ServiceRegistry registry = applicationContext.getBean("serviceRegistry", ServiceRegistry.class);
      for (ServiceContext ctx : registry.getServices()) {
         ctx.contextDestroyed(sce);
      }

      LOG.info("Shutting down Spring application context");
      applicationContext.close();
      CacheManager instance = CacheManager.getInstance();
      if (instance != null) {
         LOG.info("Stopping EH Cache Manager");
         instance.shutdown();
      }
   }
}
