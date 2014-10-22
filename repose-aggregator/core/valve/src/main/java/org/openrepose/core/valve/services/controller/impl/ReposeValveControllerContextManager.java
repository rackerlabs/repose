package org.openrepose.core.valve.services.controller.impl;

import net.sf.ehcache.CacheManager;
import org.openrepose.core.services.context.ContextAdapter;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.context.impl.ConfigurationServiceContext;
import org.openrepose.core.services.context.impl.EventManagerServiceContext;
import org.openrepose.core.services.context.impl.LoggingServiceContext;
import org.openrepose.core.services.context.impl.ReportingServiceContext;
import org.openrepose.core.services.threading.impl.ThreadingServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ReposeValveControllerContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(ReposeValveControllerContextManager.class);

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

      ServletContextHelper.configureInstance(
              servletContext,
              null);
      intializeServices(sce);

   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
       //TODO: wat
      CacheManager instance = CacheManager.getInstance();
      if (instance != null) {
         LOG.info("Stopping EH Cache Manager");
         instance.shutdown();
      }
   }
}
