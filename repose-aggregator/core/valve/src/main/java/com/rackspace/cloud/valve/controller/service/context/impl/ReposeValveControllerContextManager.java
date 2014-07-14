package com.rackspace.cloud.valve.controller.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.spring.SpringConfiguration;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * The stuff this class does should be solved by using the ContextLoaderListener.
 * Things are fired up and shut down based on when the servlet context starts and ends.
 */
@Deprecated
public class ReposeValveControllerContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(ReposeValveControllerContextManager.class);
   private AnnotationConfigApplicationContext applicationContext;

   public ReposeValveControllerContextManager() {
   }

   private void intializeServices(ServletContextEvent sce) {
      final ServletContext servletContext = sce.getServletContext();
      ServletContextHelper helper = ServletContextHelper.getInstance(sce.getServletContext());
      ContextAdapter ca = helper.getPowerApiContext();

       //This service no longer exists
      //ca.getContext(ConfigurationServiceContext.class).contextInitialized(sce);

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
