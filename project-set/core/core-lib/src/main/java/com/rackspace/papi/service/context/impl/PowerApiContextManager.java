package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.banner.PapiBanner;
import com.rackspace.papi.service.context.spring.SpringContextAdapterProvider;
import com.rackspace.papi.service.deploy.ArtifactManagerServiceContext;
import com.rackspace.papi.service.threading.impl.ThreadingServiceContext;
import com.rackspace.papi.spring.SpringConfiguration;
import java.util.LinkedList;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PowerApiContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(PowerApiContextManager.class);
   private static final String APPLICATION_CONTEXT_CONFIG = "/spring/repose-core-context.xml";
   private final LinkedList<ServiceContext> boundServiceContexts;
   private ApplicationContext applicationContext;

   public PowerApiContextManager() {
      boundServiceContexts = new LinkedList<ServiceContext>();
   }

   private <T extends ServiceContext> void initService(T resource, ServletContextEvent sce) {
      resource.contextInitialized(sce);
      boundServiceContexts.add(resource);
   }
   
   private void showBanner(ServletContextEvent sce) {
      final String showMePapi = sce.getServletContext().getInitParameter("show-me-papi");
      if (StringUtilities.nullSafeEqualsIgnoreCase(showMePapi, "true")) {
         PapiBanner.print(LOG);
      }
   }

   private void intializeServices(ServletContextEvent sce) {
      ServletContextHelper helper = ServletContextHelper.getInstance();
      ContextAdapter ca = helper.getPowerApiContext(sce.getServletContext());
      
      initService(ca.getContext(ThreadingServiceContext.class), sce);
      initService(ca.getContext(EventManagerServiceContext.class), sce);
      initService(ca.getContext(ConfigurationServiceContext.class), sce);
      initService(ca.getContext(ContainerServiceContext.class), sce);
      initService(ca.getContext(RoutingServiceContext.class), sce);
      initService(ca.getContext(LoggingServiceContext.class), sce);
      showBanner(sce);
      initService(ca.getContext(ResponseMessageServiceContext.class), sce);
      // TODO:Refactor - This service should be bound to a fitler-chain specific JNDI context
      initService(ca.getContext(DatastoreServiceContext.class), sce);
      initService(ca.getContext(ClassLoaderServiceContext.class), sce);
      initService(ca.getContext(ArtifactManagerServiceContext.class), sce);
      initService(ca.getContext(FilterChainGCServiceContext.class), sce);
      
      /*
      initService(applicationContext.getBean("threadingServiceContext", ThreadingServiceContext.class), sce);
      initService(applicationContext.getBean("eventManagerServiceContext", EventManagerServiceContext.class), sce);
      initService(applicationContext.getBean("configurationServiceContext", ConfigurationServiceContext.class), sce);
      initService(applicationContext.getBean("containerServiceContext", ContainerServiceContext.class), sce);
      initService(applicationContext.getBean("routingServiceContext", RoutingServiceContext.class), sce);
      initService(applicationContext.getBean("loggingServiceContext", LoggingServiceContext.class), sce);
      showBanner(sce);
      initService(applicationContext.getBean("responseMessageServiceContext", ResponseMessageServiceContext.class), sce);
      // TODO:Refactor - This service should be bound to a fitler-chain specific JNDI context
      initService(applicationContext.getBean("datastoreServiceContext", DatastoreServiceContext.class), sce);
      initService(applicationContext.getBean("classLoaderServiceContext", ClassLoaderServiceContext.class), sce);
      initService(applicationContext.getBean("artifactManagerServiceContext", ArtifactManagerServiceContext.class), sce);
      initService(applicationContext.getBean("filterChainGCServiceContext", FilterChainGCServiceContext.class), sce);
      * 
      */
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext servletContext = sce.getServletContext();

      // Most bootstrap steps require or will try to load some kind of
      // configuration so we need to set our naming context in the servlet context
      // first before anything else
      applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);
      //applicationContext = new ClassPathXmlApplicationContext(APPLICATION_CONTEXT_CONFIG);
      ServletContextHelper.configureInstance(
                                                       new SpringContextAdapterProvider(applicationContext), 
                                                       servletContext, 
                                                       applicationContext);

      intializeServices(sce);
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {

      for (ServiceContext ctx: boundServiceContexts) {
            ctx.contextDestroyed(sce);
      }
   }
}
