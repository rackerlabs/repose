package com.rackspace.cloud.valve.controller.service.context.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.banner.PapiBanner;
import com.rackspace.papi.service.context.impl.ConfigurationServiceContext;
import com.rackspace.papi.service.context.impl.EventManagerServiceContext;
import com.rackspace.papi.service.context.impl.LoggingServiceContext;
import com.rackspace.papi.service.context.impl.ReportingServiceContext;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.spring.SpringConfiguration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ReposeValveControllerContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(ReposeValveControllerContextManager.class);
   private AnnotationConfigApplicationContext applicationContext;
   private static final String DEFAULT_CONNECTION_FRAMEWORK = "jerseyRequestProxyService";

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
      PapiBanner.print(LOG);

      ca.getContext(ReposeValveControllerContext.class).contextInitialized(sce);

      servletContext.setAttribute("reposeValveControllerContextManager", this);
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {

      final ServletContext servletContext = sce.getServletContext();

      applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);

      final String connectionFrameworkProp = InitParameter.CONNECTION_FRAMEWORK.getParameterName();
      final String connectionFramework = System.getProperty(connectionFrameworkProp, servletContext.getInitParameter(connectionFrameworkProp));
      final String beanName = StringUtilities.isNotBlank(connectionFramework) ? connectionFramework + "RequestProxyService" : null;
      if (StringUtilities.isNotBlank(beanName) && applicationContext.containsBean(beanName)) {
         LOG.info("Using connection framework: " + beanName);
         applicationContext.registerAlias(beanName, "requestProxyService");
      } else {
         LOG.info("Using default connection framework: " + DEFAULT_CONNECTION_FRAMEWORK);
         applicationContext.registerAlias(DEFAULT_CONNECTION_FRAMEWORK, "requestProxyService");
      }
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
